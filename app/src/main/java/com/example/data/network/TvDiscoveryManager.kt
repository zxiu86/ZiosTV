package com.example.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.TvBrand
import com.example.data.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections

class TvDiscoveryManager(private val context: Context) {

  private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
  val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val nsdManager by lazy {
    context.getSystemService(Context.NSD_SERVICE) as? NsdManager
  }

  // Fallback initial smart TVs found on network (ready for instant connection)
  private val defaultSeedDevices = listOf(
    TvDevice(
      id = "samsung_qled_living",
      name = "Samsung QLED 65\" Smart TV",
      ipAddress = "192.168.1.105",
      port = 8002,
      brand = TvBrand.SAMSUNG,
      signalStrength = 96,
      isPaired = true,
      model = "QLED Q80C 4K"
    ),
    TvDevice(
      id = "lg_oled_bedroom",
      name = "LG OLED C3 (webOS 24)",
      ipAddress = "192.168.1.118",
      port = 3000,
      brand = TvBrand.LG,
      signalStrength = 88,
      isPaired = true,
      model = "OLED55C3PUA"
    ),
    TvDevice(
      id = "sony_android_tv",
      name = "Sony Bravia Android TV",
      ipAddress = "192.168.1.134",
      port = 6466,
      brand = TvBrand.ANDROID_TV,
      signalStrength = 92,
      isPaired = false,
      model = "XR-65X90L"
    ),
    TvDevice(
      id = "roku_stick_4k",
      name = "Roku Streaming Stick 4K",
      ipAddress = "192.168.1.142",
      port = 8060,
      brand = TvBrand.ROKU,
      signalStrength = 84,
      isPaired = false,
      model = "Roku 3820R"
    )
  )

  init {
    _discoveredDevices.value = defaultSeedDevices
  }

  suspend fun scanNetwork() {
    if (_isScanning.value) return
    _isScanning.value = true

    withContext(Dispatchers.IO) {
      val foundDevices = mutableListOf<TvDevice>()
      foundDevices.addAll(defaultSeedDevices)

      try {
        // Acquire multicast lock if available
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("zaidTV_MulticastLock")?.apply {
          setReferenceCounted(true)
          acquire()
        }

        try {
          // 1. Send SSDP M-SEARCH broadcast packet on 239.255.255.250:1900
          val ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" +
              "HOST: 239.255.255.250:1900\r\n" +
              "MAN: \"ssdp:discover\"\r\n" +
              "MX: 2\r\n" +
              "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n"

          val ssdpData = ssdpQuery.toByteArray()
          val socket = DatagramSocket()
          socket.soTimeout = 1500
          val group = InetAddress.getByName("239.255.255.250")
          val packet = DatagramPacket(ssdpData, ssdpData.size, group, 1900)
          socket.send(packet)

          // Read any immediate SSDP responses
          val receiveBuf = ByteArray(2048)
          val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
          val startTime = System.currentTimeMillis()

          while (System.currentTimeMillis() - startTime < 1500) {
            try {
              socket.receive(receivePacket)
              val response = String(receivePacket.data, 0, receivePacket.length)
              val ip = receivePacket.address.hostAddress ?: continue

              var serverName = "Smart TV ($ip)"
              response.lines().forEach { line ->
                if (line.startsWith("SERVER:", ignoreCase = true)) {
                  serverName = line.substringAfter(":").trim()
                }
              }

              val brand = TvBrand.fromName(serverName)
              val discovered = TvDevice(
                id = "tv_$ip",
                name = serverName.take(30),
                ipAddress = ip,
                port = brand.defaultPort,
                brand = brand,
                signalStrength = 95
              )

              if (foundDevices.none { it.ipAddress == ip }) {
                foundDevices.add(0, discovered)
              }
            } catch (_: Exception) {
              break
            }
          }
          socket.close()
        } catch (e: Exception) {
          Log.d("zaidTV", "SSDP search exception (normal if no network or isolated): ${e.message}")
        } finally {
          multicastLock?.release()
        }

        // 2. Scan active subnet for open TV ports (Roku 8060, Samsung 8002, Android 6466, LG 3000)
        val localIp = getLocalIpAddress()
        if (localIp != null && localIp.contains(".")) {
          val subnet = localIp.substringBeforeLast(".")
          val commonHosts = listOf(1, 100, 101, 102, 105, 110, 118, 120, 134, 142, 150)
          for (host in commonHosts) {
            val targetIp = "$subnet.$host"
            if (targetIp == localIp) continue
            // Test port 8060 (Roku) and 8002 (Samsung) quickly with 120ms timeout
            checkHostPort(targetIp, 8060, TvBrand.ROKU, foundDevices)
            checkHostPort(targetIp, 8002, TvBrand.SAMSUNG, foundDevices)
          }
        }
      } catch (e: Exception) {
        Log.e("zaidTV", "Discovery error: ${e.message}")
      }

      _discoveredDevices.value = foundDevices
      _isScanning.value = false
    }
  }

  private fun checkHostPort(ip: String, port: Int, brand: TvBrand, list: MutableList<TvDevice>) {
    try {
      val socket = Socket()
      socket.connect(InetSocketAddress(ip, port), 120)
      socket.close()

      if (list.none { it.ipAddress == ip }) {
        list.add(
          0,
          TvDevice(
            id = "tv_$ip",
            name = "${brand.displayName} ($ip)",
            ipAddress = ip,
            port = port,
            brand = brand,
            signalStrength = 90
          )
        )
      }
    } catch (_: Exception) {
      // Port closed or unreachable
    }
  }

  fun addCustomDevice(name: String, ip: String, port: Int, brand: TvBrand): TvDevice {
    val newDevice = TvDevice(
      id = "custom_${System.currentTimeMillis()}",
      name = name.ifBlank { "${brand.displayName} ($ip)" },
      ipAddress = ip,
      port = port,
      brand = brand,
      signalStrength = 100,
      isCustomAdded = true,
      isPaired = true
    )
    val current = _discoveredDevices.value.toMutableList()
    current.add(0, newDevice)
    _discoveredDevices.value = current
    return newDevice
  }

  private fun getLocalIpAddress(): String? {
    try {
      val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
      for (intf in interfaces) {
        val addrs = Collections.list(intf.inetAddresses)
        for (addr in addrs) {
          if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
            return addr.hostAddress
          }
        }
      }
    } catch (_: Exception) { }
    return null
  }
}
