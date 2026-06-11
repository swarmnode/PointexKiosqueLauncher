package com.pointex.provisioningqr

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val QR_CODE_URL =
    "https://github.com/swarmnode/PointexKiosqueLauncher/releases/latest/download/provisioning_qr.png"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QrScreen()
                }
            }
        }
    }
}

@Composable
private fun QrScreen() {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val image = withContext(Dispatchers.IO) {
                val connection = URL(QR_CODE_URL).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connect()
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            }
            bitmap = image.asImageBitmap()
        } catch (e: Exception) {
            error = e.message ?: "Erreur de telechargement"
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            error != null -> Text("Erreur : $error")
            else -> Text("Chargement...")
        }
    }
}
