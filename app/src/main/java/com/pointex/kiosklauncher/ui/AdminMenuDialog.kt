package com.pointex.kiosklauncher.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Menu shown once the admin PIN has been verified: lets the administrator
 * either open the system Settings app or manage Pointex apps (install a new
 * one or uninstall an existing one) via [FtpInstallScreen].
 */
@Composable
fun AdminMenuDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onManageApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Accès administrateur") },
        text = { Text("Que souhaitez-vous faire ?") },
        confirmButton = {
            TextButton(onClick = onManageApps) {
                Text("Gérer les applications Pointex")
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Ouvrir les Paramètres")
            }
        },
    )
}
