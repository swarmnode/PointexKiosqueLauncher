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
import androidx.compose.ui.unit.sp
import com.pointex.kiosklauncher.data.AdminCodeRepository

/**
 * Challenge-response dialog used to unlock administrator access: displays a
 * random 5-digit challenge ([AdminCodeRepository.newChallenge]) and calls
 * [onPinVerified] when the entered 3-digit response solves it. A wrong
 * answer shows an error and generates a fresh challenge.
 */
@Composable
fun AdminPinDialog(
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var challenge by remember { mutableStateOf(AdminCodeRepository.newChallenge()) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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

                Text(
                    text = challenge,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp),
                )

                PinPadTitle(
                    text = error ?: "Entrez le code de réponse",
                    isError = error != null,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )

                PinDots(
                    pinLength = pin.length,
                    totalDots = AdminCodeRepository.RESPONSE_LENGTH,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                PinKeypad(
                    confirmEnabled = pin.length == AdminCodeRepository.RESPONSE_LENGTH,
                    onDigit = { digit ->
                        error = null
                        if (pin.length < AdminCodeRepository.RESPONSE_LENGTH) {
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
                        val lockoutMs = AdminCodeRepository.lockoutRemainingMs(context)
                        if (lockoutMs > 0) {
                            error = lockoutMessage(lockoutMs)
                            pin = ""
                        } else if (AdminCodeRepository.verify(context, challenge, pin)) {
                            onPinVerified()
                        } else {
                            // This failure may have just started a lockout window.
                            val newLockoutMs = AdminCodeRepository.lockoutRemainingMs(context)
                            error = if (newLockoutMs > 0) lockoutMessage(newLockoutMs) else "Code incorrect"
                            pin = ""
                            challenge = AdminCodeRepository.newChallenge()
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

private fun lockoutMessage(remainingMs: Long): String {
    val seconds = (remainingMs + 999) / 1000
    return "Trop de tentatives. Réessayez dans $seconds s"
}
