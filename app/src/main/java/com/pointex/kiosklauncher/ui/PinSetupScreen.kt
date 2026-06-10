package com.pointex.kiosklauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private enum class SetupStep { ENTER, CONFIRM }

/**
 * First-run screen: forces the configuration of a 4 or 6 digit administrator
 * PIN before [HomeScreen] becomes accessible.
 */
@Composable
fun PinSetupScreen(onPinConfirmed: (String) -> Unit, modifier: Modifier = Modifier) {
    var step by remember { mutableStateOf(SetupStep.ENTER) }
    var firstPin by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val confirmEnabled = currentPin.length == 4 || currentPin.length == 6

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Configuration du code PIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Ce code protège l'accès administrateur du kiosque.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        )

        PinPadTitle(
            text = error ?: when (step) {
                SetupStep.ENTER -> "Choisissez un code à 4 ou 6 chiffres"
                SetupStep.CONFIRM -> "Confirmez le code"
            },
            isError = error != null,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        PinDots(pinLength = currentPin.length, modifier = Modifier.padding(bottom = 32.dp))

        PinKeypad(
            modifier = Modifier.widthIn(max = 320.dp),
            confirmEnabled = confirmEnabled,
            onDigit = { digit ->
                error = null
                if (currentPin.length < PIN_MAX_LENGTH) {
                    currentPin += digit
                }
            },
            onBackspace = {
                error = null
                if (currentPin.isNotEmpty()) {
                    currentPin = currentPin.dropLast(1)
                }
            },
            onConfirm = {
                when (step) {
                    SetupStep.ENTER -> {
                        firstPin = currentPin
                        currentPin = ""
                        step = SetupStep.CONFIRM
                    }

                    SetupStep.CONFIRM -> {
                        if (currentPin == firstPin) {
                            onPinConfirmed(currentPin)
                        } else {
                            error = "Les codes ne correspondent pas, recommencez"
                            firstPin = ""
                            currentPin = ""
                            step = SetupStep.ENTER
                        }
                    }
                }
            },
        )
    }
}
