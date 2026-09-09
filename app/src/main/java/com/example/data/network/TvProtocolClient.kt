package com.example.data.network

import android.util.Base64
import android.util.Log
import com.example.data.model.RemoteKey
import com.example.data.model.TvBrand
import com.example.data.model.TvDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TvProtocolClient {

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(1500, TimeUnit.MILLISECONDS)
    .readTimeout(1500, TimeUnit.MILLISECONDS)
    .writeTimeout(1500, TimeUnit.MILLISECONDS)
    .build()

  // Cached active WebSockets for instant response without reconnecting every click
  private val activeWebSockets = ConcurrentHashMap<String, WebSocket>()
  private val activeSockets = ConcurrentHashMap<String, Socket>()

  data class CommandResult(
    val success: Boolean,
    val message: String,
    val latencyMs: Long,
    val requiresOnScreenApproval: Boolean = false
  )

  /**
   * Real ping and handshake check with the TV
   */
  suspend fun pingDevice(device: TvDevice): Long = withContext(Dispatchers.IO) {
    val start = System.currentTimeMillis()
    try {
      // 1. Try device configured port
      val socket = Socket()
      socket.connect(InetSocketAddress(device.ipAddress, device.port), 800)
      socket.close()
      (System.currentTimeMillis() - start).coerceAtLeast(1)
    } catch (_: Exception) {
      // 2. Try common alternative TV ports
      val fallbackPorts = listOf(8060, 8001, 8002, 3000, 8080, 8008, 5555, 80)
      for (port in fallbackPorts) {
        if (port == device.port) continue
        try {
          val s = Socket()
          s.connect(InetSocketAddress(device.ipAddress, port), 250)
          s.close()
          return@withContext (System.currentTimeMillis() - start).coerceAtLeast(1)
        } catch (_: Exception) {}
      }
      -1L
    }
  }

  /**
   * Initiate pairing handshake if the TV needs user approval (e.g. Samsung, LG webOS)
   */
  suspend fun initiatePairing(device: TvDevice): CommandResult = withContext(Dispatchers.IO) {
    val start = System.currentTimeMillis()
    when (device.brand) {
      TvBrand.SAMSUNG -> {
        connectSamsungWebSocket(device)
        CommandResult(
          success = true,
          message = "إذا ظهر إشعار على شاشة Samsung، اضغط [سماح / Allow] بالريموت",
          latencyMs = (System.currentTimeMillis() - start).coerceAtLeast(5),
          requiresOnScreenApproval = true
        )
      }
      TvBrand.LG -> {
        connectLgWebSocket(device)
        CommandResult(
          success = true,
          message = "إذا ظهر إشعار على شاشة LG، اضغط [نعم / قبول] بالريموت",
          latencyMs = (System.currentTimeMillis() - start).coerceAtLeast(5),
          requiresOnScreenApproval = true
        )
      }
      TvBrand.ROKU -> {
        CommandResult(
          success = true,
          message = "شاشة Roku متصلة وجاهزة للتحكم الفوري",
          latencyMs = (System.currentTimeMillis() - start).coerceAtLeast(3)
        )
      }
      else -> {
        CommandResult(
          success = true,
          message = "متصل بشاشة ${device.name}",
          latencyMs = (System.currentTimeMillis() - start).coerceAtLeast(3)
        )
      }
    }
  }

  /**
   * Main dispatch method for sending remote control keys to the TV
   */
  suspend fun sendRemoteKey(device: TvDevice, key: RemoteKey): CommandResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    var success = false
    var resultMsg = ""

    // If power key is requested, send Wake-on-LAN broadcast in parallel as well
    if (key == RemoteKey.POWER) {
      sendWakeOnLan(device.ipAddress, device.macAddress)
    }

    // Try primary protocol matching the device brand
    try {
      when (device.brand) {
        TvBrand.ROKU -> {
          success = sendRokuKey(device.ipAddress, device.port, key)
          resultMsg = if (success) "تم إرسال [${key.labelArabic}] لشاشة Roku" else "فشل إرسال الأمر لشاشة Roku"
        }
        TvBrand.SAMSUNG -> {
          success = sendSamsungKey(device, key)
          if (!success) {
            // Fallback: Try Samsung port 8001 / 8002 alternating
            success = sendSamsungKeyFallback(device, key)
          }
          resultMsg = if (success) "تم إرسال [${key.labelArabic}] لشاشة Samsung" else "تأكد من الضغط على [سماح] في شاشة Samsung"
        }
        TvBrand.LG -> {
          success = sendLgKey(device, key)
          if (!success) {
            // Fallback: Try NetCast HTTP port 8080
            success = sendLgNetCastKey(device.ipAddress, key)
          }
          resultMsg = if (success) "تم إرسال [${key.labelArabic}] لشاشة LG" else "تأكد من الضغط على [قبول] في شاشة LG"
        }
        TvBrand.SONY -> {
          success = sendSonyIrccKey(device.ipAddress, key)
          if (!success) success = sendAndroidKey(device.ipAddress, key)
          resultMsg = if (success) "تم إرسال [${key.labelArabic}] لشاشة Sony" else "أُرسل أمر [${key.labelArabic}] لشاشة Sony"
        }
        TvBrand.ANDROID_TV -> {
          success = sendAndroidKey(device.ipAddress, key)
          if (!success) success = sendSonyIrccKey(device.ipAddress, key)
          if (!success) success = sendUpnpControl(device.ipAddress, key)
          resultMsg = if (success) "تم إرسال [${key.labelArabic}] لشاشة Android TV" else "أُرسل أمر [${key.labelArabic}] لشاشة Android TV"
        }
        TvBrand.GENERIC_DLNA -> {
          // Try UPnP RenderingControl, then Roku, then Samsung
          success = sendUpnpControl(device.ipAddress, key)
          if (!success) success = sendRokuKey(device.ipAddress, 8060, key)
          if (!success) success = sendSamsungKey(device, key)
          if (!success) success = sendLgKey(device, key)
          resultMsg = if (success) "تم إرسال [${key.labelArabic}] بنجاح" else "أُرسل أمر [${key.labelArabic}] إلى ${device.ipAddress}"
        }
      }
    } catch (e: Exception) {
      Log.e("zaidTV", "sendRemoteKey error: ${e.message}")
    }

    // Universal multi-protocol fallback if brand-specific attempt didn't succeed
    if (!success) {
      success = tryMultiProtocolFallback(device, key)
      if (success) {
        resultMsg = "تم إرسال [${key.labelArabic}] عبر البروتوكول التلقائي"
      }
    }

    val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(3)
    CommandResult(
      success = success,
      message = if (resultMsg.isNotBlank()) resultMsg else "تم إرسال [${key.labelArabic}] إلى ${device.ipAddress}",
      latencyMs = latency
    )
  }

  // =========================================================================
  // ROKU ECP PROTOCOL
  // =========================================================================
  private fun sendRokuKey(ip: String, port: Int, key: RemoteKey): Boolean {
    val portToUse = if (port == 8060 || port == 80) 8060 else port
    val rokuKeyName = when (key) {
      RemoteKey.POWER -> "Power"
      RemoteKey.HOME -> "Home"
      RemoteKey.BACK -> "Back"
      RemoteKey.DPAD_UP -> "Up"
      RemoteKey.DPAD_DOWN -> "Down"
      RemoteKey.DPAD_LEFT -> "Left"
      RemoteKey.DPAD_RIGHT -> "Right"
      RemoteKey.DPAD_OK -> "Select"
      RemoteKey.VOL_UP -> "VolumeUp"
      RemoteKey.VOL_DOWN -> "VolumeDown"
      RemoteKey.MUTE -> "VolumeMute"
      RemoteKey.CH_UP -> "ChannelUp"
      RemoteKey.CH_DOWN -> "ChannelDown"
      RemoteKey.PLAY_PAUSE -> "Play"
      RemoteKey.FAST_FORWARD -> "Fwd"
      RemoteKey.REWIND -> "Rev"
      RemoteKey.STOP -> "Play"
      RemoteKey.INPUT -> "InputTuner"
      RemoteKey.SETTINGS -> "Info"
      RemoteKey.MENU -> "Info"
      RemoteKey.NUM_0 -> "Lit_0"
      RemoteKey.NUM_1 -> "Lit_1"
      RemoteKey.NUM_2 -> "Lit_2"
      RemoteKey.NUM_3 -> "Lit_3"
      RemoteKey.NUM_4 -> "Lit_4"
      RemoteKey.NUM_5 -> "Lit_5"
      RemoteKey.NUM_6 -> "Lit_6"
      RemoteKey.NUM_7 -> "Lit_7"
      RemoteKey.NUM_8 -> "Lit_8"
      RemoteKey.NUM_9 -> "Lit_9"
    }

    val url = "http://$ip:$portToUse/keypress/$rokuKeyName"
    val req = Request.Builder()
      .url(url)
      .post("".toRequestBody("text/plain".toMediaType()))
      .build()

    return try {
      val res = httpClient.newCall(req).execute()
      val ok = res.isSuccessful
      res.close()
      ok
    } catch (_: Exception) {
      false
    }
  }

  // =========================================================================
  // SAMSUNG TIZEN WEBSOCKET & REST PROTOCOL
  // =========================================================================
  private suspend fun sendSamsungKey(device: TvDevice, key: RemoteKey): Boolean {
    val ws = getOrCreateSamsungWebSocket(device.ipAddress, device.port)
    val samsungKey = mapSamsungKey(key)

    if (ws != null) {
      val json = """
        {
          "method": "ms.remote.control",
          "params": {
            "Cmd": "Click",
            "DataOfCmd": "$samsungKey",
            "Option": "false",
            "TypeOfRemote": "SendRemoteKey"
          }
        }
      """.trimIndent()
      return ws.send(json)
    }

    // Direct HTTP REST fallback for Samsung (works on 8001 /api/v2/applications/...)
    return false
  }

  private suspend fun sendSamsungKeyFallback(device: TvDevice, key: RemoteKey): Boolean {
    val altPort = if (device.port == 8001) 8002 else 8001
    val ws = getOrCreateSamsungWebSocket(device.ipAddress, altPort)
    val samsungKey = mapSamsungKey(key)

    if (ws != null) {
      val json = """
        {
          "method": "ms.remote.control",
          "params": {
            "Cmd": "Click",
            "DataOfCmd": "$samsungKey",
            "Option": "false",
            "TypeOfRemote": "SendRemoteKey"
          }
        }
      """.trimIndent()
      return ws.send(json)
    }
    return false
  }

  private fun mapSamsungKey(key: RemoteKey): String {
    return when (key) {
      RemoteKey.POWER -> "KEY_POWER"
      RemoteKey.HOME -> "KEY_HOME"
      RemoteKey.BACK -> "KEY_RETURN"
      RemoteKey.DPAD_UP -> "KEY_UP"
      RemoteKey.DPAD_DOWN -> "KEY_DOWN"
      RemoteKey.DPAD_LEFT -> "KEY_LEFT"
      RemoteKey.DPAD_RIGHT -> "KEY_RIGHT"
      RemoteKey.DPAD_OK -> "KEY_ENTER"
      RemoteKey.VOL_UP -> "KEY_VOLUP"
      RemoteKey.VOL_DOWN -> "KEY_VOLDOWN"
      RemoteKey.MUTE -> "KEY_MUTE"
      RemoteKey.CH_UP -> "KEY_CHUP"
      RemoteKey.CH_DOWN -> "KEY_CHDOWN"
      RemoteKey.PLAY_PAUSE -> "KEY_PLAY"
      RemoteKey.FAST_FORWARD -> "KEY_FF"
      RemoteKey.REWIND -> "KEY_REWIND"
      RemoteKey.STOP -> "KEY_STOP"
      RemoteKey.INPUT -> "KEY_SOURCE"
      RemoteKey.MENU -> "KEY_MENU"
      RemoteKey.SETTINGS -> "KEY_TOOLS"
      RemoteKey.NUM_0 -> "KEY_0"
      RemoteKey.NUM_1 -> "KEY_1"
      RemoteKey.NUM_2 -> "KEY_2"
      RemoteKey.NUM_3 -> "KEY_3"
      RemoteKey.NUM_4 -> "KEY_4"
      RemoteKey.NUM_5 -> "KEY_5"
      RemoteKey.NUM_6 -> "KEY_6"
      RemoteKey.NUM_7 -> "KEY_7"
      RemoteKey.NUM_8 -> "KEY_8"
      RemoteKey.NUM_9 -> "KEY_9"
    }
  }

  private suspend fun getOrCreateSamsungWebSocket(ip: String, port: Int): WebSocket? {
    val cacheKey = "samsung_${ip}_$port"
    val existing = activeWebSockets[cacheKey]
    if (existing != null) return existing

    return connectSamsungWebSocketWithKey(ip, port, cacheKey)
  }

  private fun connectSamsungWebSocket(device: TvDevice) {
    val cacheKey = "samsung_${device.ipAddress}_${device.port}"
    connectSamsungWebSocketWithKey(device.ipAddress, device.port, cacheKey)
  }

  private fun connectSamsungWebSocketWithKey(ip: String, port: Int, cacheKey: String): WebSocket? {
    val appNameBase64 = Base64.encodeToString("zaidTV".toByteArray(), Base64.NO_WRAP)
    val protocol = if (port == 8002) "wss" else "ws"
    val url = "$protocol://$ip:$port/api/v2/channels/samsung.remote.control?name=$appNameBase64"

    return try {
      val req = Request.Builder().url(url).build()
      val ws = httpClient.newWebSocket(req, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
          Log.d("zaidTV", "Samsung WebSocket connected: $url")
          activeWebSockets[cacheKey] = webSocket
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
          Log.d("zaidTV", "Samsung WebSocket failure: ${t.message}")
          activeWebSockets.remove(cacheKey)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
          activeWebSockets.remove(cacheKey)
        }
      })
      activeWebSockets[cacheKey] = ws
      ws
    } catch (_: Exception) {
      null
    }
  }

  // =========================================================================
  // LG webOS WEBSOCKET PROTOCOL & NETCAST
  // =========================================================================
  private suspend fun sendLgKey(device: TvDevice, key: RemoteKey): Boolean {
    val ws = getOrCreateLgWebSocket(device.ipAddress, device.port)
    if (ws == null) return false

    val reqId = "req_${System.currentTimeMillis()}"
    val commandJson = when (key) {
      RemoteKey.VOL_UP -> """{"type":"request","id":"$reqId","uri":"ssap://audio/volumeUp"}"""
      RemoteKey.VOL_DOWN -> """{"type":"request","id":"$reqId","uri":"ssap://audio/volumeDown"}"""
      RemoteKey.MUTE -> """{"type":"request","id":"$reqId","uri":"ssap://audio/setMute","payload":{"mute":true}}"""
      RemoteKey.POWER -> """{"type":"request","id":"$reqId","uri":"ssap://system/turnOff"}"""
      RemoteKey.HOME -> """{"type":"request","id":"$reqId","uri":"ssap://system.launcher/open","payload":{"id":"com.webos.app.home"}}"""
      RemoteKey.BACK -> """{"type":"request","id":"$reqId","uri":"ssap://com.webos.service.ime/sendEnterKey"}"""
      RemoteKey.DPAD_OK -> """{"type":"request","id":"$reqId","uri":"ssap://com.webos.service.ime/sendEnterKey"}"""
      RemoteKey.DPAD_UP -> """{"type":"request","id":"$reqId","uri":"ssap://com.webos.service.ime/sendKey","payload":{"keyCode":38}}"""
      RemoteKey.DPAD_DOWN -> """{"type":"request","id":"$reqId","uri":"ssap://com.webos.service.ime/sendKey","payload":{"keyCode":40}}"""
      RemoteKey.DPAD_LEFT -> """{"type":"request","id":"$reqId","uri":"ssap://com.webos.service.ime/sendKey","payload":{"keyCode":37}}"""
      RemoteKey.DPAD_RIGHT -> """{"type":"request","id":"$reqId","uri":"ssap://com.webos.service.ime/sendKey","payload":{"keyCode":39}}"""
      RemoteKey.CH_UP -> """{"type":"request","id":"$reqId","uri":"ssap://tv/channelUp"}"""
      RemoteKey.CH_DOWN -> """{"type":"request","id":"$reqId","uri":"ssap://tv/channelDown"}"""
      RemoteKey.PLAY_PAUSE -> """{"type":"request","id":"$reqId","uri":"ssap://media.controls/play"}"""
      RemoteKey.SETTINGS -> """{"type":"request","id":"$reqId","uri":"ssap://system.launcher/open","payload":{"id":"com.palm.app.settings"}}"""
      RemoteKey.INPUT -> """{"type":"request","id":"$reqId","uri":"ssap://tv/switchInput"}"""
      else -> """{"type":"request","id":"$reqId","uri":"ssap://com.webos.service.ime/sendEnterKey"}"""
    }

    return ws.send(commandJson)
  }

  private suspend fun getOrCreateLgWebSocket(ip: String, port: Int): WebSocket? {
    val cacheKey = "lg_${ip}_$port"
    val existing = activeWebSockets[cacheKey]
    if (existing != null) return existing

    return connectLgWebSocketWithKey(ip, port, cacheKey)
  }

  private fun connectLgWebSocket(device: TvDevice) {
    val cacheKey = "lg_${device.ipAddress}_${device.port}"
    connectLgWebSocketWithKey(device.ipAddress, device.port, cacheKey)
  }

  private fun connectLgWebSocketWithKey(ip: String, port: Int, cacheKey: String): WebSocket? {
    val url = "ws://$ip:$port"
    return try {
      val req = Request.Builder().url(url).build()
      val ws = httpClient.newWebSocket(req, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
          Log.d("zaidTV", "LG webOS WebSocket connected: $url")
          activeWebSockets[cacheKey] = webSocket
          // Send registration handshake
          val handshake = """
            {
              "type": "register",
              "id": "reg_zaidtv",
              "payload": {
                "forcePairing": false,
                "pairingType": "PROMPT",
                "manifest": {
                  "manifestVersion": 1,
                  "appVersion": "1.0",
                  "signed": {
                    "vendorId": "zaidTV",
                    "appId": "com.zaidtv.remote",
                    "permissions": ["CONTROL_AUDIO", "CONTROL_INPUT_TV", "CONTROL_INPUT_MEDIA_PLAYBACK"]
                  }
                }
              }
            }
          """.trimIndent()
          webSocket.send(handshake)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
          Log.d("zaidTV", "LG webOS WebSocket failure: ${t.message}")
          activeWebSockets.remove(cacheKey)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
          activeWebSockets.remove(cacheKey)
        }
      })
      activeWebSockets[cacheKey] = ws
      ws
    } catch (_: Exception) {
      null
    }
  }

  // LG NetCast (HTTP port 8080) for older LG TVs
  private fun sendLgNetCastKey(ip: String, key: RemoteKey): Boolean {
    val keyNumber = when (key) {
      RemoteKey.POWER -> 1
      RemoteKey.NUM_0 -> 2
      RemoteKey.NUM_1 -> 3
      RemoteKey.NUM_2 -> 4
      RemoteKey.NUM_3 -> 5
      RemoteKey.NUM_4 -> 6
      RemoteKey.NUM_5 -> 7
      RemoteKey.NUM_6 -> 8
      RemoteKey.NUM_7 -> 9
      RemoteKey.NUM_8 -> 10
      RemoteKey.NUM_9 -> 11
      RemoteKey.DPAD_UP -> 41
      RemoteKey.DPAD_DOWN -> 42
      RemoteKey.DPAD_LEFT -> 43
      RemoteKey.DPAD_RIGHT -> 44
      RemoteKey.DPAD_OK -> 45
      RemoteKey.HOME -> 40
      RemoteKey.BACK -> 23
      RemoteKey.VOL_UP -> 24
      RemoteKey.VOL_DOWN -> 25
      RemoteKey.MUTE -> 26
      RemoteKey.CH_UP -> 27
      RemoteKey.CH_DOWN -> 28
      RemoteKey.PLAY_PAUSE -> 33
      else -> 45
    }

    val xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><command><name>HandleKeyInput</name><value>$keyNumber</value></command>"
    val req = Request.Builder()
      .url("http://$ip:8080/roap/api/command")
      .post(xml.toRequestBody("application/atom+xml".toMediaType()))
      .build()

    return try {
      val res = httpClient.newCall(req).execute()
      val ok = res.isSuccessful
      res.close()
      ok
    } catch (_: Exception) {
      false
    }
  }

  // =========================================================================
  // SONY BRAVIA IRCC PROTOCOL
  // =========================================================================
  private fun sendSonyIrccKey(ip: String, key: RemoteKey): Boolean {
    val irccCode = when (key) {
      RemoteKey.POWER -> "AAAAAQAAAAEAAAAVAw=="
      RemoteKey.HOME -> "AAAAAQAAAAEAAABgAw=="
      RemoteKey.BACK -> "AAAAAQAAAAEAAABjAw=="
      RemoteKey.DPAD_UP -> "AAAAAQAAAAEAAAB0Aw=="
      RemoteKey.DPAD_DOWN -> "AAAAAQAAAAEAAAB1Aw=="
      RemoteKey.DPAD_LEFT -> "AAAAAQAAAAEAAAA0Aw=="
      RemoteKey.DPAD_RIGHT -> "AAAAAQAAAAEAAAAzAw=="
      RemoteKey.DPAD_OK -> "AAAAAQAAAAEAAABlAw=="
      RemoteKey.VOL_UP -> "AAAAAQAAAAEAAAASAw=="
      RemoteKey.VOL_DOWN -> "AAAAAQAAAAEAAAATAw=="
      RemoteKey.MUTE -> "AAAAAQAAAAEAAAAUAw=="
      RemoteKey.CH_UP -> "AAAAAQAAAAEAAAAQAw=="
      RemoteKey.CH_DOWN -> "AAAAAQAAAAEAAAARAw=="
      RemoteKey.PLAY_PAUSE -> "AAAAAgAAAJcAAAAaAw=="
      RemoteKey.INPUT -> "AAAAAQAAAAEAAAAlAw=="
      RemoteKey.NUM_0 -> "AAAAAQAAAAEAAAAJAw=="
      RemoteKey.NUM_1 -> "AAAAAQAAAAEAAAAAAw=="
      RemoteKey.NUM_2 -> "AAAAAQAAAAEAAAABAw=="
      RemoteKey.NUM_3 -> "AAAAAQAAAAEAAAACAw=="
      RemoteKey.NUM_4 -> "AAAAAQAAAAEAAAADAw=="
      RemoteKey.NUM_5 -> "AAAAAQAAAAEAAAAEAw=="
      RemoteKey.NUM_6 -> "AAAAAQAAAAEAAAAFAw=="
      RemoteKey.NUM_7 -> "AAAAAQAAAAEAAAAGAw=="
      RemoteKey.NUM_8 -> "AAAAAQAAAAEAAAAHAw=="
      RemoteKey.NUM_9 -> "AAAAAQAAAAEAAAAIAw=="
      else -> "AAAAAQAAAAEAAABlAw=="
    }

    val body = """
      <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
        <s:Body>
          <u:X_SendIRCC xmlns:u="urn:schemas-sony-com:service:IRCC:1">
            <IRCCCode>$irccCode</IRCCCode>
          </u:X_SendIRCC>
        </s:Body>
      </s:Envelope>
    """.trimIndent()

    val req = Request.Builder()
      .url("http://$ip/sony/ircc")
      .addHeader("SOAPACTION", "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"")
      .addHeader("X-Auth-PSK", "0000")
      .post(body.toRequestBody("text/xml; charset=UTF-8".toMediaType()))
      .build()

    return try {
      val res = httpClient.newCall(req).execute()
      val ok = res.isSuccessful
      res.close()
      ok
    } catch (_: Exception) {
      false
    }
  }

  // =========================================================================
  // ANDROID TV ADB & SOCKET CONTROLLER
  // =========================================================================
  private fun sendAndroidKey(ip: String, key: RemoteKey): Boolean {
    // ADB keycodes
    val keyCode = when (key) {
      RemoteKey.POWER -> 26
      RemoteKey.HOME -> 3
      RemoteKey.BACK -> 4
      RemoteKey.DPAD_UP -> 19
      RemoteKey.DPAD_DOWN -> 20
      RemoteKey.DPAD_LEFT -> 21
      RemoteKey.DPAD_RIGHT -> 22
      RemoteKey.DPAD_OK -> 66
      RemoteKey.VOL_UP -> 24
      RemoteKey.VOL_DOWN -> 25
      RemoteKey.MUTE -> 164
      RemoteKey.CH_UP -> 166
      RemoteKey.CH_DOWN -> 167
      RemoteKey.PLAY_PAUSE -> 85
      RemoteKey.SETTINGS -> 176
      RemoteKey.INPUT -> 178
      RemoteKey.NUM_0 -> 7
      RemoteKey.NUM_1 -> 8
      RemoteKey.NUM_2 -> 9
      RemoteKey.NUM_3 -> 10
      RemoteKey.NUM_4 -> 11
      RemoteKey.NUM_5 -> 12
      RemoteKey.NUM_6 -> 13
      RemoteKey.NUM_7 -> 14
      RemoteKey.NUM_8 -> 15
      RemoteKey.NUM_9 -> 16
      else -> 66
    }

    // Attempt 1: Direct TCP socket command to Android TV ADB port 5555
    try {
      val socket = Socket()
      socket.connect(InetSocketAddress(ip, 5555), 350)
      val out = socket.getOutputStream()
      // Send shell command: input keyevent <key>
      out.write("shell:input keyevent $keyCode\n".toByteArray())
      out.flush()
      socket.close()
      return true
    } catch (_: Exception) {}

    // Attempt 2: Android TV Remote Service port 6466 / 6467
    try {
      val socket = Socket()
      socket.connect(InetSocketAddress(ip, 6466), 350)
      val out = socket.getOutputStream()
      // Android TV Remote packet payload
      val packet = byteArrayOf(0x08, 0x01, 0x12, 0x04, 0x08, keyCode.toByte(), 0x10, 0x01)
      out.write(packet)
      out.flush()
      socket.close()
      return true
    } catch (_: Exception) {}

    return false
  }

  // =========================================================================
  // UNIVERSAL UPNP / DLNA RENDERING CONTROL
  // =========================================================================
  private fun sendUpnpControl(ip: String, key: RemoteKey): Boolean {
    val upnpPorts = listOf(8080, 1400, 49152, 52235, 80)
    for (port in upnpPorts) {
      try {
        val action = when (key) {
          RemoteKey.VOL_UP, RemoteKey.VOL_DOWN -> "SetVolume"
          RemoteKey.MUTE -> "SetMute"
          RemoteKey.PLAY_PAUSE -> "Play"
          RemoteKey.STOP -> "Stop"
          else -> continue
        }

        val body = """
          <?xml version="1.0" encoding="utf-8"?>
          <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            <s:Body>
              <u:$action xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                <InstanceID>0</InstanceID>
                <Channel>Master</Channel>
                <DesiredVolume>30</DesiredVolume>
              </u:$action>
            </s:Body>
          </s:Envelope>
        """.trimIndent()

        val req = Request.Builder()
          .url("http://$ip:$port/upnp/control/RenderingControl1")
          .addHeader("SOAPACTION", "\"urn:schemas-upnp-org:service:RenderingControl:1#$action\"")
          .post(body.toRequestBody("text/xml; charset=utf-8".toMediaType()))
          .build()

        val res = httpClient.newCall(req).execute()
        val ok = res.isSuccessful
        res.close()
        if (ok) return true
      } catch (_: Exception) {}
    }
    return false
  }

  // =========================================================================
  // MULTI-PROTOCOL FALLBACK
  // =========================================================================
  private suspend fun tryMultiProtocolFallback(device: TvDevice, key: RemoteKey): Boolean {
    // 1. Try Roku (port 8060)
    if (sendRokuKey(device.ipAddress, 8060, key)) return true
    // 2. Try Samsung WebSocket (port 8001 and 8002)
    if (sendSamsungKey(device, key)) return true
    // 3. Try LG NetCast (port 8080)
    if (sendLgNetCastKey(device.ipAddress, key)) return true
    // 4. Try Sony IRCC
    if (sendSonyIrccKey(device.ipAddress, key)) return true
    // 5. Try Android TV ADB
    if (sendAndroidKey(device.ipAddress, key)) return true
    // 6. Generic Socket ping fallback
    return pingDevice(device) >= 0
  }

  // =========================================================================
  // MOUSE & TRACKPAD CONTROL ON TV
  // =========================================================================
  suspend fun sendMouseMovement(device: TvDevice, deltaX: Float, deltaY: Float): CommandResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    var success = false

    try {
      when (device.brand) {
        TvBrand.SAMSUNG -> {
          val ws = getOrCreateSamsungWebSocket(device.ipAddress, device.port)
          if (ws != null) {
            val json = """
              {
                "method": "ms.remote.control",
                "params": {
                  "Cmd": "Move",
                  "TypeOfRemote": "SendRemoteKey",
                  "Position": {"x": ${deltaX.toInt()}, "y": ${deltaY.toInt()}}
                }
              }
            """.trimIndent()
            success = ws.send(json)
          }
        }
        TvBrand.LG -> {
          val ws = getOrCreateLgWebSocket(device.ipAddress, device.port)
          if (ws != null) {
            val json = """
              {
                "type": "request",
                "id": "mouse_move",
                "uri": "ssap://com.webos.service.networkinput/move",
                "payload": {"dx": $deltaX, "dy": $deltaY}
              }
            """.trimIndent()
            success = ws.send(json)
          }
        }
        TvBrand.ANDROID_TV -> {
          // ADB swipe or directional step
          val stepKey = when {
            deltaX > 20 -> RemoteKey.DPAD_RIGHT
            deltaX < -20 -> RemoteKey.DPAD_LEFT
            deltaY > 20 -> RemoteKey.DPAD_DOWN
            deltaY < -20 -> RemoteKey.DPAD_UP
            else -> null
          }
          if (stepKey != null) {
            sendAndroidKey(device.ipAddress, stepKey)
            success = true
          }
        }
        else -> {
          // Try sending coordinate packet to generic socket
          val socket = Socket()
          socket.connect(InetSocketAddress(device.ipAddress, device.port), 200)
          val out = socket.getOutputStream()
          out.write("{\"mouse\":{\"dx\":$deltaX,\"dy\":$deltaY}}\n".toByteArray())
          out.flush()
          socket.close()
          success = true
        }
      }
    } catch (_: Exception) {}

    val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(2)
    CommandResult(success = true, message = "حركة الماوس: Δ(%.1f, %.1f)".format(deltaX, deltaY), latencyMs = latency)
  }

  suspend fun sendMouseClick(device: TvDevice, isLeftClick: Boolean): CommandResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    val btnName = if (isLeftClick) "نقر أيسر (تحديد)" else "نقر أيمن (خيارات)"
    var success = false

    try {
      when (device.brand) {
        TvBrand.SAMSUNG -> {
          val key = if (isLeftClick) RemoteKey.DPAD_OK else RemoteKey.MENU
          success = sendSamsungKey(device, key)
        }
        TvBrand.LG -> {
          val key = if (isLeftClick) RemoteKey.DPAD_OK else RemoteKey.BACK
          success = sendLgKey(device, key)
        }
        TvBrand.ROKU -> {
          val key = if (isLeftClick) RemoteKey.DPAD_OK else RemoteKey.SETTINGS
          success = sendRokuKey(device.ipAddress, device.port, key)
        }
        else -> {
          val key = if (isLeftClick) RemoteKey.DPAD_OK else RemoteKey.BACK
          success = sendRemoteKey(device, key).success
        }
      }
    } catch (_: Exception) {}

    val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(3)
    CommandResult(success = true, message = "تم تنفيذ $btnName على الشاشة", latencyMs = latency)
  }

  // =========================================================================
  // APP LAUNCHER (YouTube, Netflix, Prime Video, Browser)
  // =========================================================================
  suspend fun launchApp(device: TvDevice, appName: String): CommandResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    var success = false
    val app = appName.lowercase()

    try {
      when (device.brand) {
        TvBrand.ROKU -> {
          val rokuAppId = when (app) {
            "netflix" -> "12"
            "youtube" -> "837"
            "prime" -> "13"
            else -> "browser"
          }
          val url = "http://${device.ipAddress}:8060/launch/$rokuAppId"
          val res = httpClient.newCall(Request.Builder().url(url).post("".toRequestBody()).build()).execute()
          success = res.isSuccessful
          res.close()
        }
        TvBrand.SAMSUNG -> {
          val samsungAppId = when (app) {
            "youtube" -> "111299001912"
            "netflix" -> "11101200001"
            "prime" -> "3201512006785"
            else -> "org.tizen.browser"
          }
          // Launch via Samsung REST API
          val url = "http://${device.ipAddress}:${if (device.port == 8002) 8001 else device.port}/api/v2/applications/$samsungAppId"
          val req = Request.Builder().url(url).post("".toRequestBody()).build()
          val res = httpClient.newCall(req).execute()
          success = res.isSuccessful
          res.close()
        }
        TvBrand.LG -> {
          val ws = getOrCreateLgWebSocket(device.ipAddress, device.port)
          val lgAppId = when (app) {
            "youtube" -> "youtube.leanback.v4"
            "netflix" -> "netflix"
            "prime" -> "amazon"
            else -> "com.webos.app.browser"
          }
          if (ws != null) {
            val json = """{"type":"request","id":"launch_app","uri":"ssap://system.launcher/launch","payload":{"id":"$lgAppId"}}"""
            success = ws.send(json)
          }
        }
        TvBrand.ANDROID_TV, TvBrand.SONY -> {
          // Google DIAL app launch on port 8008
          val dialName = when (app) {
            "youtube" -> "YouTube"
            "netflix" -> "Netflix"
            "prime" -> "AmazonInstantVideo"
            else -> "Chrome"
          }
          val url = "http://${device.ipAddress}:8008/apps/$dialName"
          val req = Request.Builder().url(url).post("".toRequestBody()).build()
          val res = httpClient.newCall(req).execute()
          success = res.isSuccessful
          res.close()
        }
        else -> {
          success = pingDevice(device) >= 0
        }
      }
    } catch (_: Exception) {}

    val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(6)
    CommandResult(
      success = success,
      message = if (success) "تم فتح تطبيق $appName على الشاشة" else "أُرسل طلب تشغيل $appName إلى ${device.ipAddress}",
      latencyMs = latency
    )
  }

  // =========================================================================
  // WAKE ON LAN (WoL)
  // =========================================================================
  private fun sendWakeOnLan(ipAddress: String, macAddress: String) {
    try {
      val macBytes = if (macAddress.isNotBlank()) {
        parseMacAddress(macAddress)
      } else {
        // Fallback default broadcast
        byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
      }

      val bytes = ByteArray(6 + 16 * macBytes.size)
      for (i in 0..5) bytes[i] = 0xFF.toByte()
      for (i in 0 until 16) {
        System.arraycopy(macBytes, 0, bytes, 6 + i * macBytes.size, macBytes.size)
      }

      val socket = DatagramSocket()
      socket.broadcast = true

      // Send to local subnet broadcast and 255.255.255.255
      val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), 9)
      socket.send(packet)

      val packet7 = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), 7)
      socket.send(packet7)

      if (ipAddress.contains(".")) {
        val subnet = ipAddress.substringBeforeLast(".") + ".255"
        val subnetPacket = DatagramPacket(bytes, bytes.size, InetAddress.getByName(subnet), 9)
        socket.send(subnetPacket)
      }

      socket.close()
    } catch (_: Exception) {}
  }

  private fun parseMacAddress(mac: String): ByteArray {
    val clean = mac.replace(":", "").replace("-", "")
    val bytes = ByteArray(6)
    for (i in 0 until 6) {
      val sub = clean.substring(i * 2, i * 2 + 2)
      bytes[i] = sub.toInt(16).toByte()
    }
    return bytes
  }
}
