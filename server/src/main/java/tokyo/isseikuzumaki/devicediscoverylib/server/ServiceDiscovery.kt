package tokyo.isseikuzumaki.devicediscoverylib.server

import android.content.Context

interface ServiceDiscovery {
    companion object {
        fun build(
            context: Context,
            name: String,
            transportProtocol: Server.TransportProtocol,
            transportLayer: Server.TransportLayer,
            transportPort: Server.TransportPort
        ): ServiceDiscovery {
            throw NotImplementedError()
        }
    }
    fun start()
    fun stop()
}
