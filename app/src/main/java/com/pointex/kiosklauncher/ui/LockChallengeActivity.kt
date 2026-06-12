package com.pointex.kiosklauncher.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pointex.kiosklauncher.MainActivity
import com.pointex.kiosklauncher.data.WatchdogRepository
import com.pointex.kiosklauncher.ui.theme.PointexKioskLauncherTheme

/**
 * Full-screen gate shown by [com.pointex.kiosklauncher.admin.KioskWatchdogService]
 * when a gated app reaches the foreground in limited kiosk mode. Covers that
 * app and demands the admin challenge-response code.
 *
 * Correct code → [WatchdogRepository.temporarilyUnlocked] is set and the
 * activity finishes, revealing the gated app for the technician (the gate
 * re-arms when the kiosk next regains the foreground). Back / cancel returns
 * to the kiosk instead.
 */
class LockChallengeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PointexKioskLauncherTheme {
                LockChallengeScreen(
                    onVerified = {
                        WatchdogRepository.temporarilyUnlocked = true
                        finish()
                    },
                    onCancel = ::returnToKiosk,
                )
            }
        }
    }

    override fun onBackPressed() {
        returnToKiosk()
    }

    private fun returnToKiosk() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }
}

@Composable
private fun LockChallengeScreen(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Accès protégé",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Entrez le code administrateur pour continuer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            ChallengeCodeEntry(
                onVerified = onVerified,
                modifier = Modifier.padding(top = 24.dp),
            )

            androidx.compose.material3.TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Retour au kiosque")
            }
        }
    }
}
