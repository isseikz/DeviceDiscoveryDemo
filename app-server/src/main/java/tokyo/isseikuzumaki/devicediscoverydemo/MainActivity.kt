package tokyo.isseikuzumaki.devicediscoverydemo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
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
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val PORT = 23084
    }

    private lateinit var server: Server
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
            }
        }.build(this@MainActivity)


        val bitmap = getLocalIpAddress()?.let {
            "http://$it:$PORT"
        }?.let {
            MultiFormatWriter().encode(it, BarcodeFormat.QR_CODE, 256, 256)
        }?.let {
            BarcodeEncoder().createBitmap(it)
        }


        setContent {
            DeviceDiscoveryDemoTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Row {
                        AppName(name = getString(R.string.app_name))
                    }
                    Row {
                        bitmap?.let {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Ipaddress"
                            )
                        }
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
    Text(text = name)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DeviceDiscoveryDemoTheme {
        AppName("AppName")
    }
}

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
