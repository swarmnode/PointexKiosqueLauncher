package com.pointex.kiosklauncher.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device admin receiver for PointexKioskLauncher.
 *
 * When the app is Device Owner (see `adb shell dpm set-device-owner ...`)
 * this enables the strict lock-task restrictions in
 * [com.pointex.kiosklauncher.admin.KioskPolicyManager]. In limited kiosk
 * mode the same receiver is activated as a plain device admin (not owner)
 * so the app cannot be uninstalled through the normal paths until the admin
 * is deactivated — [onDisableRequested] warns the user before that happens.
 */
class KioskAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device admin enabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? =
        "Désactiver l'administrateur retire la protection du mode kiosque et " +
            "permet de désinstaller l'application Pointex. À ne faire que pour " +
            "la maintenance, sur instruction d'un technicien."

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Device admin disabled")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.i(TAG, "Entering lock task mode for package: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.i(TAG, "Exiting lock task mode")
    }

    companion object {
        private const val TAG = "KioskAdminReceiver"
    }
}
