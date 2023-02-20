package tokyo.isseikuzumaki.devicediscoverylib.server

import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.devicediscoverylib.server.protocol.Http
import tokyo.isseikuzumaki.devicediscoverylib.server.protocol.HttpHost
import tokyo.isseikuzumaki.devicediscoverylib.server.servicediscovery.NetworkServiceDiscovery
import java.net.ServerSocket

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
        var extras: Bundle? = null
        fun build(context: Context): Server {
            val serviceDiscovery = when(discoveryMethod) {
                ServiceDiscoveryMethod.DNS_SERVICE_DISCOVERY -> {
                    NetworkServiceDiscovery.build(context, name, transportProtocol, transportLayer, transportPort)
                }
            }
            val protocol = when(transportProtocol) {
                TransportProtocol.HTTP -> {
                    Http(transportPort.number, context.cacheDir)
                }
                TransportProtocol.HTTP_HOSTING -> {
                    HttpHost(transportPort.number, context.filesDir)
                }
                else -> {
                    throw NotImplementedError()
                }
            }
            return Server(serviceDiscovery, protocol, protocolEventListener)
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
            val AUTO = fromAvailablePort()
            fun custom(number: Int) = TransportPort(number)
            private fun fromAvailablePort(): TransportPort {
                val port = ServerSocket(0).also{
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
