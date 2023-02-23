package tokyo.isseikuzumaki.devicediscoverylib.server

import android.net.Uri
import java.io.File

interface ProtocolEventListener {
    fun onProtocolEstablished(uri: Uri) {}
    suspend fun onRequestText(uri: Uri): String?
    suspend fun onRequestFile(uri: Uri): File?
    suspend fun onPostFiles(files: List<File>): String?
}
