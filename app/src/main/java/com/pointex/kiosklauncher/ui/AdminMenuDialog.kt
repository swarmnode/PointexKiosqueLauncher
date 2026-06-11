package com.pointex.kiosklauncher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Menu shown once the admin PIN has been verified: lets the administrator
 * open various system Settings screens (general settings, Wi-Fi, SIM card)
 * or manage Pointex apps (install a new one or uninstall an existing one)
 * via [FtpInstallScreen].
 */
@Composable
fun AdminMenuDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenSimSettings: () -> Unit,
    onManageApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Accès administrateur") },
        text = {
            Column {
                TextButton(onClick = onManageApps, modifier = Modifier.fillMaxWidth()) {
                    Text("Gérer les applications Pointex")
                }
                TextButton(onClick = onOpenWifiSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Wi-Fi (adresse IP fixe)")
                }
                TextButton(onClick = onOpenSimSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Carte SIM")
                }
                TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Ouvrir les Paramètres")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
    )
}
