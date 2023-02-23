package tokyo.isseikuzumaki.devicediscoverylib.server

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.devicediscoverylib.server.protocol.Http
import tokyo.isseikuzumaki.devicediscoverylib.server.protocol.HttpHost
import tokyo.isseikuzumaki.devicediscoverylib.server.servicediscovery.NetworkServiceDiscovery
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketException

class Server(
    private val serviceDiscovery: ServiceDiscovery,
    private val protocol: Protocol,
    private val protocolEventListener: ProtocolEventListener?
) {
    private val serverScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        serverScope.launch {
            protocol.start(protocolEventListener)
            serviceDiscovery.start()
        }
    }

    fun stop() {
        serverScope.launch {
            serviceDiscovery.stop()
            protocol.stop()
        }
    }

    class Builder {
        lateinit var name: String
        lateinit var transportProtocol: TransportProtocol
        lateinit var transportLayer: TransportLayer
        lateinit var discoveryMethod: ServiceDiscoveryMethod
        lateinit var transportPort: TransportPort
        var protocolEventListener: ProtocolEventListener? = null
        fun build(context: Context): Server {
            val serviceDiscovery = when (discoveryMethod) {
                ServiceDiscoveryMethod.DNS_SERVICE_DISCOVERY -> {
                    NetworkServiceDiscovery.build(
                        context,
                        name,
                        transportProtocol,
                        transportLayer,
                        transportPort
                    )
                }
            }
            getLocalIpAddress()?.let { host ->
                when (transportProtocol) {
                    TransportProtocol.HTTP -> {
                        Http(host, transportPort.number, context.cacheDir)
                    }
                    TransportProtocol.HTTP_HOSTING -> {
                        HttpHost(host, transportPort.number, context.filesDir)
                    }
                    else -> {
                        throw NotImplementedError()
                    }
                }
            }?.let { protocol ->
                return Server(serviceDiscovery, protocol, protocolEventListener)
            } ?: throw IllegalStateException("Network connection required.")
        }

        companion object {
            fun getLocalIpAddress(): String? {
                try {
                    val enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces()
                    while (enumNetworkInterfaces.hasMoreElements()) {
                        val networkInterface = enumNetworkInterfaces.nextElement()
                        val enumInetAddress = networkInterface.inetAddresses
                        while (enumInetAddress.hasMoreElements()) {
                            val inetAddress = enumInetAddress.nextElement()
                            if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                return inetAddress.hostAddress
                            }
                        }
                    }
                } catch (e: SocketException) {
                    e.printStackTrace()
                }
                return null
            }
        }
    }

    data class TransportProtocol(val name: String, val type: String = "") {
        companion object {
            val MULTICAST_DNS = TransportProtocol("mdns")
            val HTTP = TransportProtocol("http")
            val HTTP_HOSTING = TransportProtocol("http", "hosting")
            fun custom(name: String) = TransportProtocol(name)
        }
    }

    data class TransportLayer(val name: String) {
        companion object {
            val TCP = TransportLayer("tcp")
            val UDP = TransportLayer("udp")
            fun custom(name: String) = TransportLayer(name)
        }
    }

    data class TransportPort(val number: Int) {
        companion object {
            val AUTO = 0
            fun custom(number: Int) = TransportPort(number)
            private fun fromAvailablePort(): TransportPort {
                val port = ServerSocket(0).also {
                    it.close()
                }.localPort
                return TransportPort(port)
            }
        }
    }

    enum class ServiceDiscoveryMethod {
        DNS_SERVICE_DISCOVERY
    }
}
