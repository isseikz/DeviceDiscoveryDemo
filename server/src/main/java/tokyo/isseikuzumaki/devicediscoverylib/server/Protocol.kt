package tokyo.isseikuzumaki.devicediscoverylib.server


interface Protocol {
    val scheme: String
    fun start(listener: ProtocolEventListener? = null)
    fun stop()
}
