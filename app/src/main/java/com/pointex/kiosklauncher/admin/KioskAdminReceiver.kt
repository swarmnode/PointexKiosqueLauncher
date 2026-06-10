package com.pointex.kiosklauncher.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device admin receiver for PointexKioskLauncher.
 *
 * The application must be provisioned as Device Owner (see
 * `adb shell dpm set-device-owner ...`) for the strict lock-task
 * restrictions configured in [com.pointex.kiosklauncher.admin.KioskPolicyManager]
 * to take effect.
 */
class KioskAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device admin enabled")
    }

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
