package com.pointex.kiosklauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pointex.kiosklauncher.admin.KioskPolicyManager
import com.pointex.kiosklauncher.data.BootLaunchRepository
import com.pointex.kiosklauncher.data.KioskAppRepository
import com.pointex.kiosklauncher.data.KioskModeRepository
import com.pointex.kiosklauncher.ui.KioskApp
import com.pointex.kiosklauncher.ui.theme.PointexKioskLauncherTheme

/**
 * Single-activity kiosk launcher entry point.
 *
 * Re-applies lock-task mode on every resume, which both pins the activity
 * on first launch and re-locks the kiosk automatically when the
 * administrator returns from the temporarily unlocked Settings screen.
 *
 * Additionally, on the first resume after a device boot, when the kiosk is
 * configured and exactly one Pointex app is installed, that app is launched
 * automatically on top. As Device Owner the app is allowlisted and opens
 * over the still-locked kiosk; in limited mode nothing is pinned, so it just
 * opens normally.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PointexKioskLauncherTheme {
                KioskApp(activity = this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pins the kiosk only as Device Owner; a safe no-op in limited mode.
        KioskPolicyManager.enterLockTask(this)

        val configured = KioskPolicyManager.isDeviceOwner(this) ||
            KioskModeRepository.isLimitedModeChosen(this)
        if (configured && BootLaunchRepository.shouldAutoLaunchAfterBoot(this)) {
            KioskAppRepository.getAllowedApps(this).singleOrNull()?.let { app ->
                BootLaunchRepository.markAutoLaunched(this)
                startActivity(KioskAppRepository.launchIntentFor(app))
            }
        }
    }
}
