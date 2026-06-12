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

/**
 * First-run screen shown when the app has not been provisioned as Device
 * Owner (e.g. a Google account was still present on the device, the
 * QR-code provisioning step was skipped, or another app already holds
 * Device Owner on this device). [onRetry] re-checks
 * `KioskPolicyManager.isDeviceOwner()`, while [onContinueAnyway] proceeds
 * in a degraded "limited kiosk" mode (screen pinning instead of full
 * lock-task lockdown) for devices that can never become Device Owner.
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
                "Certaines protections (verrouillage complet, barre de statut, Wi-Fi/Paramètres) " +
                "ne pourront pas être appliquées tant que cette configuration n'est pas effectuée.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .widthIn(max = 480.dp),
        )

        Text(
            text = "Pour une configuration complète :\n" +
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

        TextButton(onClick = onContinueAnyway, modifier = Modifier.padding(top = 8.dp)) {
            Text("Configurer en mode kiosque limité")
        }

        Text(
            text = "Mode limité : l'application est simplement épinglée à l'écran (un retrait " +
                "reste possible via Retour + Récents), les installations demandent une " +
                "confirmation et les protections complètes du mode Device Owner ne " +
                "s'appliquent pas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .widthIn(max = 480.dp),
        )
    }
}
