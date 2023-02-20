package tokyo.isseikuzumaki.devicediscoverylib.server.servicediscovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import tokyo.isseikuzumaki.devicediscoverylib.server.Server
import tokyo.isseikuzumaki.devicediscoverylib.server.ServiceDiscovery
import java.net.InetAddress

class NetworkServiceDiscovery(
    private val manager: NsdManager,
    private val info: NsdServiceInfo,
    private val listener: NsdManager.RegistrationListener
): ServiceDiscovery {
    companion object {
        const val TAG = "NetworkServiceDiscovery"
        fun build(
            context: Context,
            name: String,
            transportProtocol: Server.TransportProtocol,
            transportLayer: Server.TransportLayer,
            transportPort: Server.TransportPort
        ): ServiceDiscovery {
            val manager = context.getSystemService(NsdManager::class.java)
            val info = NsdServiceInfo().apply {
                serviceName = name
                serviceType = "_${transportProtocol.name}._${transportLayer.name}"
                port = transportPort.number
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.e(TAG, "onRegistrationFailed: ${createErrorMessage(errorCode)}")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.e(TAG, "onUnregistrationFailed: ${createErrorMessage(errorCode)}")
                }

                override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                    Log.d(TAG, "onServiceRegistered")
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                    Log.d(TAG, "onServiceUnregistered")
                }

                private fun createErrorMessage(errorCode: Int): String {
                    return when (errorCode) {
                        NsdManager.FAILURE_INTERNAL_ERROR -> "internal error"
                        NsdManager.FAILURE_ALREADY_ACTIVE -> "already active"
                        NsdManager.FAILURE_MAX_LIMIT -> "max limit"
                        else -> "unknown"
                    }
                }
            }
            return NetworkServiceDiscovery(manager, info, listener)
        }
    }

    override fun start() {
        manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    override fun stop() {
        manager.unregisterService(listener)
    }
}
