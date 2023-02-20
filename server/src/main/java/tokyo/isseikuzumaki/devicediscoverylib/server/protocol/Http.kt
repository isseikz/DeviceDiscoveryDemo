package tokyo.isseikuzumaki.devicediscoverylib.server.protocol

import android.net.Uri
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.devicediscoverylib.server.Protocol
import tokyo.isseikuzumaki.devicediscoverylib.server.ProtocolEventListener
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

class Http(port: Int, cacheDir: File) : Protocol {
    override val scheme = "http"
    private val engine: ApplicationEngine
    private var listener: ProtocolEventListener? = null
    private val eventDispatcher = Dispatchers.IO

    init {
        engine = embeddedServer(Netty, port = port) {
            install(AutoHeadResponse)
            routing {
                get {
                    withContext(eventDispatcher) {
                        listener?.onRequestFile(Uri.parse(call.request.uri))
                    }?.let {
                        call.respondFile(it)
                    } ?: run {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                get("/data/*") {
                    withContext(eventDispatcher) {
                        listener?.onRequestFile(Uri.parse(call.request.uri))
                    }?.let {
                        call.respondFile(it)
                    } ?: run {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                post("/upload") {
                    val files = mutableListOf<File>()
                    var description = ""
                    var name = ""
                    call.receiveMultipart().forEachPart {
                        when (it) {
                            is PartData.FormItem -> {
                                description = it.value
                            }
                            is PartData.FileItem -> {
                                name = it.originalFileName.toString()
                                it.streamProvider().use { input ->
                                    File(cacheDir, name).also { file ->
                                        files.add(file)
                                    }.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            else -> {}
                        }
                        it.dispose()
                    }
                    withContext(eventDispatcher) {
                        listener?.onPostFiles(files)
                    }?.let {
                        call.respondText(it)
                    } ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                    }
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
