package com.pointex.kiosklauncher.data

import android.content.Context

/**
 * Configuration and runtime state for the access watchdog
 * ([com.pointex.kiosklauncher.admin.KioskWatchdogService]): which foreground
 * apps trigger the full-screen admin-code prompt.
 *
 * The watchdog only matters in limited kiosk mode (no Device Owner), where
 * the status bar / Recents stay reachable; under Device Owner lock-task
 * already blocks everything.
 */
object WatchdogRepository {

    private const val PREFS_FILE_NAME = "pointex_kiosk_secure_prefs"
    private const val KEY_SCOPE = "watchdog_scope"

    enum class Scope {
        /** Watchdog off — nothing is gated. */
        DISABLED,

        /** Only the system Settings app is gated behind the admin code. */
        SETTINGS_ONLY,

        /** Everything that is not the kiosk, an allowed app or an essential system package. */
        ALL_NON_ALLOWED,
    }

    /**
     * Packages the kiosk may launch (refreshed from `KioskApp`), so the
     * watchdog never gates an allowed Pointex/Fiducial app. Read on the
     * accessibility thread, written from the UI — kept as a volatile snapshot.
     */
    @Volatile
    var allowedPackages: Set<String> = emptySet()

    /**
     * Set true once the admin code has been entered, so the watchdog lets the
     * technician use the gated app freely; cleared when the kiosk regains the
     * foreground (protection re-arms).
     */
    @Volatile
    var temporarilyUnlocked: Boolean = false

    fun scope(context: Context): Scope =
        runCatching {
            Scope.valueOf(
                SecurePrefs.get(context, PREFS_FILE_NAME).getString(KEY_SCOPE, Scope.DISABLED.name)!!
            )
        }.getOrDefault(Scope.DISABLED)

    fun setScope(context: Context, scope: Scope) {
        SecurePrefs.get(context, PREFS_FILE_NAME).edit().putString(KEY_SCOPE, scope.name).apply()
    }
}
