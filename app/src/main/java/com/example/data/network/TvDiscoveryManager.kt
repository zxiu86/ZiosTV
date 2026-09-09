package com.example.data.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.TvBrand
import com.example.data.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections
import java.util.concurrent.TimeUnit

class TvDiscoveryManager(private val context: Context) {

  private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
  val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _networkInfo = MutableStateFlow("جارٍ التحقق من الشبكة...")
  val networkInfo: StateFlow<String> = _networkInfo.asStateFlow()

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(600, TimeUnit.MILLISECONDS)
    .readTimeout(800, TimeUnit.MILLISECONDS)
    .build()

  init {
    updateNetworkStatus()
  }

  fun updateNetworkStatus(): String {
    val localIp = getLocalIpAddress()
    val info = if (localIp != null) {
      val subnet = localIp.substringBeforeLast(".")
      "Wi-Fi متصل: $localIp (النطاق المحلي $subnet.0/24)"
    } else {
      "غير متصل بشبكة Wi-Fi محلية"
    }
    _networkInfo.value = info
    return info
  }

  suspend fun scanNetwork() {
    if (_isScanning.value) return
    _isScanning.value = true

    withContext(Dispatchers.IO) {
      val localIp = getLocalIpAddress()
      updateNetworkStatus()

      val foundDevices = mutableListOf<TvDevice>()
      // Keep any user-added custom devices
      val customDevices = _discoveredDevices.value.filter { it.isCustomAdded }
      foundDevices.addAll(customDevices)

      // Acquire MulticastLock for SSDP if Wi-Fi manager is available
      val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      val multicastLock = try {
        wifiManager?.createMulticastLock("zaidTV_MulticastLock")?.apply {
          setReferenceCounted(true)
          acquire()
        }
      } catch (_: Exception) {
        null
      }

      try {
        // 1. Real SSDP / UPnP M-SEARCH broadcast on local network
        searchSsdpDevices(foundDevices)

        // 2. Real Parallel Subnet Probe if local IP is present
        if (localIp != null && localIp.contains(".")) {
          val subnetPrefix = localIp.substringBeforeLast(".")
          scanSubnetRange(subnetPrefix, localIp, foundDevices)
        }
      } catch (e: Exception) {
        Log.e("zaidTV", "Error during network scan: ${e.message}")
      } finally {
        try {
          multicastLock?.release()
        } catch (_: Exception) {}
      }

      _discoveredDevices.value = foundDevices.toList()
      _isScanning.value = false
    }
  }

  private fun searchSsdpDevices(targetList: MutableList<TvDevice>) {
    try {
      val ssdpQueries = listOf(
        "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 1\r\n" +
            "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n",
        "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 1\r\n" +
            "ST: roku:ecp\r\n\r\n",
        "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 1\r\n" +
            "ST: urn:dial-multiscreen-org:service:dial:1\r\n\r\n"
      )

      val socket = DatagramSocket()
      socket.soTimeout = 900
      val group = InetAddress.getByName("239.255.255.250")

      for (query in ssdpQueries) {
        val data = query.toByteArray()
        val packet = DatagramPacket(data, data.size, group, 1900)
        socket.send(packet)
      }

      val buffer = ByteArray(4096)
      val receivePacket = DatagramPacket(buffer, buffer.size)
      val startTime = System.currentTimeMillis()

      while (System.currentTimeMillis() - startTime < 1200) {
        try {
          socket.receive(receivePacket)
          val response = String(receivePacket.data, 0, receivePacket.length)
          val ip = receivePacket.address.hostAddress ?: continue

          if (targetList.any { it.ipAddress == ip }) continue

          var locationUrl: String? = null
          var serverHeader: String? = null

          response.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("LOCATION:", ignoreCase = true)) {
              locationUrl = trimmed.substringAfter(":").trim()
            }
            if (trimmed.startsWith("SERVER:", ignoreCase = true)) {
              serverHeader = trimmed.substringAfter(":").trim()
            }
          }

          var tvName = serverHeader ?: "Smart TV ($ip)"
          var brand = TvBrand.fromName(tvName)
          var defaultPort = brand.defaultPort

          // If location URL is provided, query the real UPnP XML description
          if (!locationUrl.isNullOrBlank()) {
            try {
              val portFromUrl = locationUrl?.substringAfter("://")?.substringBefore("/")?.substringAfter(":")?.toIntOrNull()
              if (portFromUrl != null) defaultPort = portFromUrl

              val req = Request.Builder().url(locationUrl!!).build()
              val res = httpClient.newCall(req).execute()
              val xml = res.body?.string() ?: ""
              res.close()

              val parsedName = extractXmlTag(xml, "friendlyName")
              val parsedManufacturer = extractXmlTag(xml, "manufacturer")
              val parsedModel = extractXmlTag(xml, "modelName")

              if (!parsedName.isNullOrBlank()) {
                tvName = parsedName
                brand = TvBrand.fromName("$tvName $parsedManufacturer")
              }
            } catch (_: Exception) {}
          }

          val discovered = TvDevice(
            id = "tv_$ip",
            name = tvName.take(35),
            ipAddress = ip,
            port = defaultPort,
            brand = brand,
            signalStrength = 95
          )

          targetList.add(0, discovered)
          _discoveredDevices.value = targetList.toList()
        } catch (_: Exception) {
          break
        }
      }
      socket.close()
    } catch (e: Exception) {
      Log.d("zaidTV", "SSDP discovery note: ${e.message}")
    }
  }

  private suspend fun scanSubnetRange(subnet: String, localIp: String, targetList: MutableList<TvDevice>) = withContext(Dispatchers.IO) {
    // Smart TV candidate ports to probe: Roku(8060), Samsung(8001/8002), Chromecast/AndroidTV(8008), webOS(3000)
    val portsToCheck = listOf(8060, 8001, 8002, 8008, 3000, 6466, 8080)
    val candidateHosts = (1..254).filter { "$subnet.$it" != localIp }

    // Scan in concurrent batches of 25 hosts
    candidateHosts.chunked(25).forEach { chunk ->
      val jobs = chunk.map { hostNum ->
        async {
          val ip = "$subnet.$hostNum"
          if (targetList.none { it.ipAddress == ip }) {
            for (port in portsToCheck) {
              if (isPortOpen(ip, port, 180)) {
                val realDevice = probeDeviceDetails(ip, port)
                synchronized(targetList) {
                  if (targetList.none { it.ipAddress == ip }) {
                    targetList.add(0, realDevice)
                    _discoveredDevices.value = targetList.toList()
                  }
                }
                break
              }
            }
          }
        }
      }
      jobs.awaitAll()
    }
  }

  private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
    return try {
      val socket = Socket()
      socket.connect(InetSocketAddress(ip, port), timeoutMs)
      socket.close()
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun probeDeviceDetails(ip: String, port: Int): TvDevice {
    // Determine TV brand and real name based on port and HTTP query
    when (port) {
      8060 -> {
        // Roku TV ECP - fetch real device info
        try {
          val req = Request.Builder().url("http://$ip:8060/query/device-info").build()
          val res = httpClient.newCall(req).execute()
          val xml = res.body?.string() ?: ""
          res.close()
          val userDevName = extractXmlTag(xml, "user-device-name")
          val modelName = extractXmlTag(xml, "model-name")
          val devName = when {
            !userDevName.isNullOrBlank() -> userDevName
            !modelName.isNullOrBlank() -> "Roku $modelName"
            else -> "Roku TV"
          }
          return TvDevice(
            id = "roku_$ip",
            name = devName,
            ipAddress = ip,
            port = port,
            brand = TvBrand.ROKU,
            signalStrength = 92,
            model = modelName ?: "Roku TV"
          )
        } catch (_: Exception) {}
        return TvDevice(id = "roku_$ip", name = "Roku TV ($ip)", ipAddress = ip, port = port, brand = TvBrand.ROKU)
      }
      8001, 8002 -> {
        // Samsung Tizen Smart TV
        try {
          val req = Request.Builder().url("http://$ip:$port/api/v2/").build()
          val res = httpClient.newCall(req).execute()
          val body = res.body?.string() ?: ""
          res.close()
          var name = "Samsung Smart TV"
          if (body.contains("\"name\":")) {
            name = body.substringAfter("\"name\":\"").substringBefore("\"")
          }
          return TvDevice(
            id = "samsung_$ip",
            name = name,
            ipAddress = ip,
            port = port,
            brand = TvBrand.SAMSUNG,
            signalStrength = 95
          )
        } catch (_: Exception) {}
        return TvDevice(id = "samsung_$ip", name = "Samsung Smart TV ($ip)", ipAddress = ip, port = port, brand = TvBrand.SAMSUNG)
      }
      8008 -> {
        // Google Cast / Android TV DIAL
        try {
          val req = Request.Builder().url("http://$ip:8008/setup/eureka_info").build()
          val res = httpClient.newCall(req).execute()
          val body = res.body?.string() ?: ""
          res.close()
          var name = "Google Cast / Android TV"
          if (body.contains("\"name\":")) {
            name = body.substringAfter("\"name\":\"").substringBefore("\"")
          }
          return TvDevice(
            id = "android_tv_$ip",
            name = name,
            ipAddress = ip,
            port = 6466,
            brand = TvBrand.ANDROID_TV,
            signalStrength = 90
          )
        } catch (_: Exception) {}
        return TvDevice(id = "android_tv_$ip", name = "Android TV ($ip)", ipAddress = ip, port = 6466, brand = TvBrand.ANDROID_TV)
      }
      3000, 3001 -> {
        return TvDevice(id = "lg_$ip", name = "LG webOS TV ($ip)", ipAddress = ip, port = port, brand = TvBrand.LG)
      }
      else -> {
        return TvDevice(id = "tv_$ip", name = "Smart TV ($ip)", ipAddress = ip, port = port, brand = TvBrand.GENERIC_DLNA)
      }
    }
  }

  private fun extractXmlTag(xml: String, tag: String): String? {
    val open = "<$tag>"
    val close = "</$tag>"
    if (xml.contains(open) && xml.contains(close)) {
      return xml.substringAfter(open).substringBefore(close).trim()
    }
    return null
  }

  suspend fun addCustomDevice(name: String, ip: String, port: Int, brand: TvBrand): TvDevice = withContext(Dispatchers.IO) {
    val cleanIp = ip.trim()
    val cleanPort = if (port > 0) port else brand.defaultPort
    val cleanName = name.ifBlank { "${brand.displayName} ($cleanIp)" }

    // Real ping check
    val reachable = isPortOpen(cleanIp, cleanPort, 500)
    val newDevice = TvDevice(
      id = "custom_${System.currentTimeMillis()}",
      name = cleanName,
      ipAddress = cleanIp,
      port = cleanPort,
      brand = brand,
      signalStrength = if (reachable) 98 else 75,
      isCustomAdded = true,
      isPaired = true
    )

    val current = _discoveredDevices.value.toMutableList()
    current.removeAll { it.ipAddress == cleanIp }
    current.add(0, newDevice)
    _discoveredDevices.value = current
    newDevice
  }

  fun getLocalIpAddress(): String? {
    try {
      val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
      for (intf in interfaces) {
        if (!intf.isUp || intf.isLoopback) continue
        val addrs = Collections.list(intf.inetAddresses)
        for (addr in addrs) {
          if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
            val host = addr.hostAddress
            if (host != null && (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172."))) {
              return host
            }
          }
        }
      }
    } catch (_: Exception) { }
    return null
  }
}
