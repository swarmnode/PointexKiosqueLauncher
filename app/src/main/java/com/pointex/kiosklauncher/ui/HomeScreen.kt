package com.pointex.kiosklauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pointex.kiosklauncher.data.KioskApp

/**
 * Main kiosk screen: the allowed ("pointex"/"fiducial") apps, shown as
 * clickable tiles centered on screen (wraps to multiple rows if needed).
 * Tapping a tile invokes [onAppClick]. A discreet long-press area in the
 * bottom-right corner invokes [onAdminRequested] to open the admin PIN dialog.
 */
@Composable
fun HomeScreen(
    apps: List<KioskApp>,
    onAppClick: (KioskApp) -> Unit,
    onAdminRequested: () -> Unit,
    onInstallRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (apps.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = 0f, verticalBias = -0.4f))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Aucune application disponible",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Aucune application \"pointex\" ou \"fiducial\" n'est installée sur cet appareil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(onClick = onInstallRequested, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Installer une application Pointex")
                }
            }
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                apps.forEach { app ->
                    AppTile(app = app, onClick = { onAppClick(app) })
                }
            }
        }

        // Discreet admin trigger: long-press the bottom-right corner.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .size(56.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onAdminRequested() })
                }
        )
    }
}

@Composable
private fun AppTile(app: KioskApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val iconBitmap = remember(app.packageName) {
            app.icon.toBitmap(width = 128, height = 128).asImageBitmap()
        }
        Image(
            bitmap = iconBitmap,
            contentDescription = app.label,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
