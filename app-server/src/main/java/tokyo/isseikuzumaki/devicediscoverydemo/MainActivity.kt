package tokyo.isseikuzumaki.devicediscoverydemo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.devicediscoverydemo.ui.theme.DeviceDiscoveryDemoTheme
import tokyo.isseikuzumaki.devicediscoverylib.server.ProtocolEventListener
import tokyo.isseikuzumaki.devicediscoverylib.server.Server
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val PORT = 23084
    }

    private lateinit var server: Server
    val qrBitmap: MutableState<ImageBitmap?> = mutableStateOf(null)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        server = Server.Builder().apply {
            name = "Device Discovery Demo"
            transportPort = Server.TransportPort.custom(PORT)
            transportProtocol = Server.TransportProtocol.HTTP
            transportLayer = Server.TransportLayer.TCP
            discoveryMethod = Server.ServiceDiscoveryMethod.DNS_SERVICE_DISCOVERY
            protocolEventListener = object : ProtocolEventListener {
                override suspend fun onRequestText(uri: Uri): String? {
                    uiScope.launch {
                        Toast.makeText(this@MainActivity, uri.toString(), Toast.LENGTH_SHORT).show()
                    }
                    Log.d(TAG, "onRequestText: $uri")
                    return "Received"
                }

                override suspend fun onRequestFile(uri: Uri): File? {
                    uiScope.launch {
                        Toast.makeText(this@MainActivity, uri.toString(), Toast.LENGTH_SHORT).show()
                    }
                    Log.d(TAG, "onRequestFile: $uri")
                    return uri.path?.let { File(filesDir, it) }?.takeIf { it.exists() }?.let {
                        if (it.isFile) it else File(it, "index.html")
                    }?.takeIf { it.exists() }
                }

                override suspend fun onPostFiles(files: List<File>): String? {
                    uiScope.launch {
                        Toast.makeText(
                            this@MainActivity,
                            "${files.size} file(s) have been uploaded! ${files.map { it.name }}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return "OK"
                }

                override fun onProtocolEstablished(uri: Uri) {
                    MultiFormatWriter()
                        .encode(uri.toString(), BarcodeFormat.QR_CODE, 256, 256)
                        .let { BarcodeEncoder().createBitmap(it) }
                        .let { qrBitmap.value = it.asImageBitmap() }
                }
            }
        }.build(this@MainActivity)

        setContent {
            DeviceDiscoveryDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AppName(name = getString(R.string.app_name))
                        QR_ControlPanel(qrState = qrBitmap)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        server.start()
    }

    override fun onStop() {
        server.stop()
        super.onStop()
    }
}

@Composable
fun AppName(name: String) {
    Text(text = name, fontSize = 20.sp)
    Spacer(modifier = Modifier.width(width = 60.dp))
}

@Composable
fun QR_ControlPanel(qrState: MutableState<ImageBitmap?>) {
    Text(text = "Read QR code to access control panel")
    qrState.value?.let {
        Image(
            bitmap = it,
            contentDescription = "Ipaddress"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DeviceDiscoveryDemoTheme {
        AppName("AppName")
    }
}
