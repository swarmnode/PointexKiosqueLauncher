package com.pointex.kiosklauncher.data

import android.content.Context

/**
 * Stores the kiosk administrator PIN inside [SecurePrefs]. A 4-digit PIN on
 * a publicly exposed kiosk is brute-forceable, so failed attempts are rate
 * limited: after [MAX_ATTEMPTS] consecutive failures, PIN entry is locked
 * for [LOCKOUT_DURATION_MS].
 */
object PinRepository {

    private const val PREFS_FILE_NAME = "pointex_kiosk_secure_prefs"
    private const val KEY_ADMIN_PIN = "admin_pin"
    private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"

    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L

    /** True once an administrator PIN has been configured. */
    fun isPinSet(context: Context): Boolean =
        SecurePrefs.get(context, PREFS_FILE_NAME).contains(KEY_ADMIN_PIN)

    /** Stores [pin] (4 or 6 digits) as the administrator PIN. */
    fun setPin(context: Context, pin: String) {
        SecurePrefs.get(context, PREFS_FILE_NAME).edit().putString(KEY_ADMIN_PIN, pin).apply()
    }

    /** Milliseconds before PIN entry is allowed again, or 0 if not locked out. */
    fun lockoutRemainingMs(context: Context): Long {
        val until = SecurePrefs.get(context, PREFS_FILE_NAME).getLong(KEY_LOCKOUT_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * Returns true if [pin] matches the stored administrator PIN. Always
     * false while locked out (check [lockoutRemainingMs] first to tell the
     * user why). A failed attempt increments the failure counter; reaching
     * [MAX_ATTEMPTS] starts a new lockout window.
     */
    fun verifyPin(context: Context, pin: String): Boolean {
        if (lockoutRemainingMs(context) > 0) return false

        val prefs = SecurePrefs.get(context, PREFS_FILE_NAME)
        val matches = prefs.getString(KEY_ADMIN_PIN, null) == pin
        if (matches) {
            prefs.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKOUT_UNTIL).apply()
        } else {
            val failures = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit()
            if (failures >= MAX_ATTEMPTS) {
                editor.putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
            } else {
                editor.putInt(KEY_FAILED_ATTEMPTS, failures)
            }
            editor.apply()
        }
        return matches
    }
}
