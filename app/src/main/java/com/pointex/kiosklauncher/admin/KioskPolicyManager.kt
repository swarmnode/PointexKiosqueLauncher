package com.pointex.kiosklauncher.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.UserManager
import android.util.Log

/**
 * Wraps the [DevicePolicyManager] calls needed to run PointexKioskLauncher
 * as a strict Device Owner kiosk: pinning the launcher via lock-task mode
 * and locking down the status bar, Home, Recents and Settings access.
 *
 * All calls are defensive: if the app has not (yet) been provisioned as
 * Device Owner, every function is a safe no-op so the UI can still be
 * exercised during development.
 */
object KioskPolicyManager {

    private const val TAG = "KioskPolicyManager"

    /** Settings app package, temporarily allowed during admin unlock (see [exitLockTask]). */
    private const val SETTINGS_PACKAGE = "com.android.settings"

    /** Restrictions applied while the kiosk is locked down. */
    private val USER_RESTRICTIONS = listOf(
        UserManager.DISALLOW_SAFE_BOOT,
        UserManager.DISALLOW_FACTORY_RESET,
        UserManager.DISALLOW_ADD_USER,
        UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA,
    )

    /** Non-kiosk packages allowed under lock-task mode, set via [updateLockTaskPackages]. */
    @Volatile
    private var allowedPackages: List<String> = emptyList()

    /** This app plus [allowedPackages] plus any [extra] packages, deduplicated. */
    private fun lockTaskPackages(context: Context, vararg extra: String): Array<String> =
        (listOf(context.packageName) + allowedPackages + extra).distinct().toTypedArray()

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context.applicationContext, KioskAdminReceiver::class.java)

    private fun devicePolicyManager(context: Context): DevicePolicyManager =
        context.applicationContext.getSystemService(DevicePolicyManager::class.java)

    /** True once `adb shell dpm set-device-owner` has been run for this app. */
    fun isDeviceOwner(context: Context): Boolean =
        devicePolicyManager(context).isDeviceOwnerApp(context.packageName)

    /**
     * Applies the strict kiosk restrictions: pins this app as the only
     * lock-task package, disables the status bar / Home / Recents / Settings
     * shortcuts, disables the keyguard and blocks factory reset, safe boot,
     * new users and external storage mounts.
     *
     * No-op (with a warning log) if the app is not Device Owner.
     */
    fun applyKioskRestrictions(context: Context) {
        val dpm = devicePolicyManager(context)
        val admin = adminComponent(context)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "App is not Device Owner; skipping kiosk restrictions")
            return
        }

        try {
            dpm.setLockTaskPackages(admin, lockTaskPackages(context))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // LOCK_TASK_FEATURE_NONE blocks Home, Recents, notifications/
                // status bar, system info and the global actions menu while
                // lock-task mode is active.
                dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            } else {
                @Suppress("DEPRECATION")
                dpm.setStatusBarDisabled(admin, true)
            }

            dpm.setKeyguardDisabled(admin, true)

            for (restriction in USER_RESTRICTIONS) {
                dpm.addUserRestriction(admin, restriction)
            }

            Log.i(TAG, "Kiosk restrictions applied")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to apply kiosk restrictions", e)
        }
    }

    /**
     * Reverts the restrictions applied by [applyKioskRestrictions], used
     * while an administrator is temporarily out of the kiosk (see
     * `stopLockTask`/`startLockTask` in [enterLockTask]/[exitLockTask]).
     */
    fun releaseKioskRestrictions(context: Context) {
        val dpm = devicePolicyManager(context)
        val admin = adminComponent(context)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setLockTaskFeatures(
                    admin,
                    DevicePolicyManager.LOCK_TASK_FEATURE_HOME or
                        DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW or
                        DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS or
                        DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO or
                        DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD or
                        DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS
                )
            } else {
                @Suppress("DEPRECATION")
                dpm.setStatusBarDisabled(admin, false)
            }

            // Keep the keyguard disabled even outside lock-task mode: this device
            // has no Android lock screen credential configured, so re-enabling it
            // here just shows the lock/notification screen on top of Settings.

            for (restriction in USER_RESTRICTIONS) {
                dpm.clearUserRestriction(admin, restriction)
            }

            Log.i(TAG, "Kiosk restrictions released")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to release kiosk restrictions", e)
        }
    }

    /**
     * Updates the lock-task package allowlist to this app plus
     * [allowedPackages], so those apps can be launched (via
     * [android.app.Activity.startActivity]) without breaking out of
     * lock-task mode. No-op if the app is not Device Owner.
     */
    fun updateLockTaskPackages(context: Context, allowedApps: List<String>) {
        val dpm = devicePolicyManager(context)
        val admin = adminComponent(context)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            return
        }

        allowedPackages = allowedApps
        try {
            dpm.setLockTaskPackages(admin, lockTaskPackages(context))
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to update lock task packages", e)
        }
    }

    /**
     * Applies [applyKioskRestrictions] and pins [activity] via
     * `startLockTask()`. Safe to call even if the app is not Device Owner
     * (it simply won't pin the activity).
     */
    fun enterLockTask(activity: Activity) {
        applyKioskRestrictions(activity)
        ensureWifiEnabled(activity)
        try {
            activity.startLockTask()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "startLockTask failed", e)
        }
    }

    /**
     * Turns Wi-Fi back on if it has been left disabled, so the kiosk keeps
     * looking for a network to sync licenses/apps over. `setWifiEnabled` is a
     * no-op for regular apps since API 29, but device owners are exempt.
     */
    @Suppress("DEPRECATION")
    fun ensureWifiEnabled(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java) ?: return
        if (!wifiManager.isWifiEnabled) {
            try {
                wifiManager.isWifiEnabled = true
                Log.i(TAG, "Wi-Fi was disabled; re-enabled it")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to enable Wi-Fi", e)
            }
        }
    }

    /**
     * Releases the kiosk restrictions and unpins [activity] via
     * `stopLockTask()`, allowing temporary admin access. Temporarily adds
     * the Settings app to the lock-task allowlist so it can be opened from
     * a Device Owner-pinned Home task; [enterLockTask] removes it again on
     * the next resume.
     */
    fun exitLockTask(activity: Activity) {
        val dpm = devicePolicyManager(activity)
        val admin = adminComponent(activity)

        if (dpm.isDeviceOwnerApp(activity.packageName)) {
            try {
                dpm.setLockTaskPackages(admin, lockTaskPackages(activity, SETTINGS_PACKAGE))
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to allow Settings during admin unlock", e)
            }
        }

        try {
            activity.stopLockTask()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "stopLockTask failed", e)
        }
        releaseKioskRestrictions(activity)
    }
}
