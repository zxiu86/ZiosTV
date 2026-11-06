package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

data class DiscoveredRoom(
    val roomCode: String,
    val hostIp: String,
    val hostPort: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class UdpDiscoveryManager(private val context: Context) {

    private val tag = "UdpDiscovery"
    private val broadcastPort = 8888
    private val broadcastPrefix = "SOUAL_JAWAB_ROOM"

    private var broadcastJob: Job? = null
    private var listenerJob: Job? = null
    private var broadcastSocket: DatagramSocket? = null
    private var listenerSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun startBroadcasting(scope: CoroutineScope, roomCode: String, serverPort: Int) {
        stopBroadcasting()
        broadcastJob = scope.launch(Dispatchers.IO) {
            try {
                broadcastSocket = DatagramSocket().apply {
                    broadcast = true
                }
                val message = "$broadcastPrefix:$roomCode:$serverPort"
                val buffer = message.toByteArray()
                val broadcastAddress = InetAddress.getByName("255.255.255.255")

                Log.d(tag, "Starting room beacon broadcast for room: $roomCode on port $broadcastPort")
                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size, broadcastAddress, broadcastPort)
                        broadcastSocket?.send(packet)
                    } catch (e: Exception) {
                        Log.w(tag, "Broadcast send error: ${e.message}")
                    }
                    delay(1500)
                }
            } catch (e: Exception) {
                Log.e(tag, "Broadcast setup error: ${e.message}")
            } finally {
                broadcastSocket?.close()
                broadcastSocket = null
            }
        }
    }

    fun stopBroadcasting() {
        broadcastJob?.cancel()
        broadcastJob = null
        try {
            broadcastSocket?.close()
        } catch (_: Exception) {}
        broadcastSocket = null
    }

    fun startListening(
        scope: CoroutineScope,
        onRoomFound: (DiscoveredRoom) -> Unit
    ) {
        stopListening()
        acquireMulticastLock()

        listenerJob = scope.launch(Dispatchers.IO) {
            try {
                listenerSocket = DatagramSocket(broadcastPort).apply {
                    broadcast = true
                    reuseAddress = true
                }
                val buffer = ByteArray(1024)

                Log.d(tag, "Listening for room broadcasts on port $broadcastPort")
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        listenerSocket?.receive(packet)
                        val text = String(packet.data, 0, packet.length).trim()
                        if (text.startsWith(broadcastPrefix)) {
                            val parts = text.split(":")
                            if (parts.size >= 3) {
                                val roomCode = parts[1]
                                val port = parts[2].toIntOrNull() ?: 8080
                                val senderIp = packet.address.hostAddress ?: ""
                                if (senderIp.isNotEmpty()) {
                                    val room = DiscoveredRoom(
                                        roomCode = roomCode,
                                        hostIp = senderIp,
                                        hostPort = port
                                    )
                                    onRoomFound(room)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.w(tag, "Receive error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Listener error: ${e.message}")
            } finally {
                listenerSocket?.close()
                listenerSocket = null
                releaseMulticastLock()
            }
        }
    }

    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
        try {
            listenerSocket?.close()
        } catch (_: Exception) {}
        listenerSocket = null
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("SoualJawabMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    if (intf.isLoopback || !intf.isUp) continue
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                            val ip = addr.hostAddress ?: ""
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return ip
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            return "127.0.0.1"
        }
    }
}
