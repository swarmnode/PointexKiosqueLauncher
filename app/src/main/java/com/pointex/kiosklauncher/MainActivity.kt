package com.pointex.kiosklauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pointex.kiosklauncher.admin.KioskPolicyManager
import com.pointex.kiosklauncher.ui.KioskApp
import com.pointex.kiosklauncher.ui.theme.PointexKioskLauncherTheme

/**
 * Single-activity kiosk launcher entry point.
 *
 * Re-applies lock-task mode on every resume, which both pins the activity
 * on first launch and re-locks the kiosk automatically when the
 * administrator returns from the temporarily unlocked Settings screen.
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
        KioskPolicyManager.enterLockTask(this)
    }
}
