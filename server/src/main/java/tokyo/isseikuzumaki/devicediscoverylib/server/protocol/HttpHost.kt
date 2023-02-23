package tokyo.isseikuzumaki.devicediscoverylib.server.protocol

import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import tokyo.isseikuzumaki.devicediscoverylib.server.Protocol
import tokyo.isseikuzumaki.devicediscoverylib.server.ProtocolEventListener
import java.io.File

class HttpHost(
    override val host: String,
    override val port: Int,
    rootDir: File,
    default: String? = null
) : Protocol {
    override val scheme = "http"
    private val engine: ApplicationEngine
    private var listener: ProtocolEventListener? = null

    init {
        engine = embeddedServer(Netty, host = host, port = port) {
            routing {
                static("/") {
                    staticRootFolder = rootDir
                    default(default ?: "index.html")
                    files(".")
                }
            }
        }
    }

    override fun start(listener: ProtocolEventListener?) {
        this.listener = listener
        engine.start()
    }

    override fun stop() {
        engine.stop()
        this.listener = null
    }
}
