package com.pointex.kiosklauncher.data

import android.content.Context

/**
 * Challenge-response admin access code, replacing the old stored PIN.
 *
 * When admin access is requested, the dialog displays a random 5-digit
 * challenge; the valid response is the nine's complement of its 3 middle
 * digits (e.g. challenge 81634 -> middle digits 163 -> response 836). The
 * "secret" is the rule itself, known to technicians only — no per-device
 * PIN to configure or distribute, and the expected response changes with
 * every challenge.
 *
 * Brute force on 3 digits is deterred the same way as before: after
 * [MAX_ATTEMPTS] consecutive failures, entry is locked for
 * [LOCKOUT_DURATION_MS] (persisted, survives a reboot).
 */
object AdminCodeRepository {

    private const val PREFS_FILE_NAME = "pointex_kiosk_secure_prefs"
    private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"

    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L

    const val CHALLENGE_LENGTH = 5
    const val RESPONSE_LENGTH = 3

    /** Returns a new random [CHALLENGE_LENGTH]-digit challenge to display. */
    fun newChallenge(): String =
        (1..CHALLENGE_LENGTH).map { ('0'..'9').random() }.joinToString("")

    /** Nine's complement of the [RESPONSE_LENGTH] middle digits of [challenge]. */
    internal fun expectedResponse(challenge: String): String =
        challenge.substring(1, 1 + RESPONSE_LENGTH)
            .map { digit -> '0' + (9 - (digit - '0')) }
            .joinToString("")

    /** Milliseconds before code entry is allowed again, or 0 if not locked out. */
    fun lockoutRemainingMs(context: Context): Long {
        val until = SecurePrefs.get(context, PREFS_FILE_NAME).getLong(KEY_LOCKOUT_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * Returns true if [response] solves [challenge]. Always false while
     * locked out (check [lockoutRemainingMs] first to tell the user why).
     * A failed attempt increments the failure counter; reaching
     * [MAX_ATTEMPTS] starts a new lockout window.
     */
    fun verify(context: Context, challenge: String, response: String): Boolean {
        if (lockoutRemainingMs(context) > 0) return false

        val prefs = SecurePrefs.get(context, PREFS_FILE_NAME)
        val matches = response == expectedResponse(challenge)
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
