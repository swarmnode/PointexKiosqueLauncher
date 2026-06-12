package com.pointex.kiosklauncher.admin

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.pointex.kiosklauncher.data.WatchdogRepository
import com.pointex.kiosklauncher.ui.LockChallengeActivity

/**
 * Access watchdog for limited kiosk mode (no Device Owner). Watches which app
 * comes to the foreground and, when a gated app appears (per
 * [WatchdogRepository.scope]), covers it with [LockChallengeActivity], which
 * demands the admin challenge-response code.
 *
 * Requires the user to enable the service once in Accessibility settings —
 * the only no-Device-Owner way to observe foreground apps. Returning to the
 * kiosk re-arms the gate (see [WatchdogRepository.temporarilyUnlocked]).
 */
class KioskWatchdogService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        val scope = WatchdogRepository.scope(this)
        if (scope == WatchdogRepository.Scope.DISABLED) return

        // Our own windows (kiosk home, the lock prompt) are always allowed;
        // the kiosk returning to the foreground re-arms the gate.
        if (pkg == packageName) {
            WatchdogRepository.temporarilyUnlocked = false
            return
        }
        if (WatchdogRepository.temporarilyUnlocked) return

        val gated = when (scope) {
            WatchdogRepository.Scope.SETTINGS_ONLY -> pkg == SETTINGS_PACKAGE
            WatchdogRepository.Scope.ALL_NON_ALLOWED ->
                pkg !in SYSTEM_WHITELIST && pkg !in WatchdogRepository.allowedPackages
            WatchdogRepository.Scope.DISABLED -> false
        }
        if (!gated) return

        try {
            startActivity(
                Intent(this, LockChallengeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch lock prompt over $pkg", e)
        }
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "KioskWatchdogService"
        private const val SETTINGS_PACKAGE = "com.android.settings"

        /**
         * Essential system packages never gated in ALL_NON_ALLOWED mode:
         * the shell UI, the IME, permission prompts and the package installer
         * (needed for the limited-mode install confirmation). Note that
         * `com.android.settings` is intentionally absent — it is gated.
         */
        private val SYSTEM_WHITELIST = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings.intelligence",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
        )
    }
}
