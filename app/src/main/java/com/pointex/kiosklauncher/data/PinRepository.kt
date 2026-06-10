package com.pointex.kiosklauncher.data

import android.content.Context

/**
 * Stores the kiosk administrator PIN inside [SecurePrefs].
 */
object PinRepository {

    private const val PREFS_FILE_NAME = "pointex_kiosk_secure_prefs"
    private const val KEY_ADMIN_PIN = "admin_pin"

    /** True once an administrator PIN has been configured. */
    fun isPinSet(context: Context): Boolean =
        SecurePrefs.get(context, PREFS_FILE_NAME).contains(KEY_ADMIN_PIN)

    /** Stores [pin] (4 or 6 digits) as the administrator PIN. */
    fun setPin(context: Context, pin: String) {
        SecurePrefs.get(context, PREFS_FILE_NAME).edit().putString(KEY_ADMIN_PIN, pin).apply()
    }

    /** Returns true if [pin] matches the stored administrator PIN. */
    fun verifyPin(context: Context, pin: String): Boolean =
        SecurePrefs.get(context, PREFS_FILE_NAME).getString(KEY_ADMIN_PIN, null) == pin
}
