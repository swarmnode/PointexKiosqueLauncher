package com.pointex.kiosklauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KioskColorScheme = darkColorScheme(
    primary = KioskPrimary,
    onPrimary = KioskOnPrimary,
    background = KioskBackground,
    onBackground = KioskOnSurface,
    surface = KioskSurface,
    onSurface = KioskOnSurface,
    onSurfaceVariant = KioskOnSurfaceVariant,
    error = KioskError,
)

/** Always-dark Material 3 theme for the kiosk launcher. */
@Composable
fun PointexKioskLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KioskColorScheme,
        content = content,
    )
}
