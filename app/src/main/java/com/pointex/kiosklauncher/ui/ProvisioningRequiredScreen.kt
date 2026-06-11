package com.pointex.kiosklauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pointex.kiosklauncher.BuildConfig

/**
 * Blocking first-run screen shown when the app has not been provisioned as
 * Device Owner (e.g. a Google account was still present on the device, or
 * the QR-code provisioning step was skipped). Setup cannot continue until
 * [onRetry] confirms `KioskPolicyManager.isDeviceOwner()` is true.
 */
@Composable
fun ProvisioningRequiredScreen(
    onRetry: () -> Unit,
    onContinueAnyway: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp).padding(bottom = 16.dp),
        )

        Text(
            text = "Configuration requise",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Cet appareil n'est pas configuré en mode kiosque (Device Owner manquant). " +
                "L'application ne peut pas démarrer tant que cette configuration n'est pas effectuée.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .widthIn(max = 480.dp),
        )

        Text(
            text = "Pour corriger :\n" +
                "1. Réinitialisez l'appareil aux paramètres d'usine.\n" +
                "2. Lors de la configuration initiale, scannez le QR code de provisioning " +
                "(voir fiche technicien).",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .widthIn(max = 480.dp),
        )

        Button(onClick = onRetry) {
            Text("Vérifier à nouveau")
        }

        if (BuildConfig.DEBUG) {
            TextButton(onClick = onContinueAnyway, modifier = Modifier.padding(top = 8.dp)) {
                Text("Continuer sans Device Owner (debug)")
            }
        }
    }
}
