package com.pointex.kiosklauncher.data

import android.content.Context

/**
 * Remembers that the administrator explicitly chose the degraded "limited
 * kiosk" mode (screen pinning, no Device Owner) on the provisioning screen.
 * Persisted so the choice survives the activity recreation triggered by
 * granting the HOME role (becoming the default launcher restarts the app)
 * as well as reboots.
 */
object KioskModeRepository {

    private const val PREFS_FILE_NAME = "pointex_kiosk_secure_prefs"
    private const val KEY_LIMITED_MODE = "limited_kiosk_mode_chosen"

    /** True once the administrator picked "Configurer en mode kiosque limité". */
    fun isLimitedModeChosen(context: Context): Boolean =
        SecurePrefs.get(context, PREFS_FILE_NAME).getBoolean(KEY_LIMITED_MODE, false)

    fun setLimitedModeChosen(context: Context) {
        SecurePrefs.get(context, PREFS_FILE_NAME).edit().putBoolean(KEY_LIMITED_MODE, true).apply()
    }
}
