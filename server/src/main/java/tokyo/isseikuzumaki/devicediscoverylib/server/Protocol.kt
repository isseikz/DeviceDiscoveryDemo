package tokyo.isseikuzumaki.devicediscoverylib.server


interface Protocol {
    val scheme: String
    val host: String
    val port: Int
    fun start(listener: ProtocolEventListener? = null)
    fun stop()
}
