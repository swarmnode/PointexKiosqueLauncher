package com.pointex.kiosklauncher.data

import android.content.Context
import android.os.SystemClock

/**
 * Tracks whether the single allowed Pointex app has already been
 * auto-launched since the last device boot, so the kiosk opens it once at
 * startup but never forces the user back into it when they deliberately
 * return to the kiosk home.
 *
 * Boot detection compares [SystemClock.elapsedRealtime] (milliseconds since
 * boot, monotonic) with the value stored at the previous auto-launch: a
 * smaller current value can only mean the device has rebooted. This is
 * immune to wall-clock changes (NTP sync shortly after boot).
 */
object BootLaunchRepository {

    private const val PREFS_FILE_NAME = "pointex_kiosk_secure_prefs"
    private const val KEY_LAST_LAUNCH_ELAPSED = "auto_launch_elapsed_realtime"

    /** True if no auto-launch has happened yet during the current boot. */
    fun shouldAutoLaunchAfterBoot(context: Context): Boolean {
        val stored = SecurePrefs.get(context, PREFS_FILE_NAME).getLong(KEY_LAST_LAUNCH_ELAPSED, -1L)
        return stored == -1L || SystemClock.elapsedRealtime() < stored
    }

    /** Records that the auto-launch happened for the current boot. */
    fun markAutoLaunched(context: Context) {
        SecurePrefs.get(context, PREFS_FILE_NAME).edit()
            .putLong(KEY_LAST_LAUNCH_ELAPSED, SystemClock.elapsedRealtime())
            .apply()
    }
}
