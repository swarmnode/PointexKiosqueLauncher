package com.pointex.kiosklauncher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pointex.kiosklauncher.data.PinRepository

/**
 * PIN-entry dialog used to unlock administrator access. Calls
 * [onPinVerified] when the entered PIN matches [PinRepository], otherwise
 * shows an error and clears the entry.
 */
@Composable
fun AdminPinDialog(
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.widthIn(max = 360.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Accès administrateur",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                PinPadTitle(
                    text = error ?: "Entrez le code PIN",
                    isError = error != null,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                )

                PinDots(pinLength = pin.length, modifier = Modifier.padding(bottom = 24.dp))

                PinKeypad(
                    confirmEnabled = pin.length == 4 || pin.length == 6,
                    onDigit = { digit ->
                        error = null
                        if (pin.length < PIN_MAX_LENGTH) {
                            pin += digit
                        }
                    },
                    onBackspace = {
                        error = null
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                        }
                    },
                    onConfirm = {
                        if (PinRepository.verifyPin(context, pin)) {
                            onPinVerified()
                        } else {
                            error = "Code incorrect"
                            pin = ""
                        }
                    },
                )

                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Annuler")
                }
            }
        }
    }
}
