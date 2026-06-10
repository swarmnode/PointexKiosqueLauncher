package com.pointex.kiosklauncher.data

import android.content.Context

/** Saved SFTP connection details: server address plus credentials. */
data class SftpCredentials(val host: String, val username: String, val password: String)

/**
 * Stores the SFTP connection details used to browse and download Pointex apps
 * from [PointexSftpRepository], inside [SecurePrefs] so they can be reused on
 * subsequent visits without prompting the user again.
 */
object FtpCredentialsRepository {

    /** Pre-filled server address shown when no connection has been saved yet. */
    const val DEFAULT_HOST = "ftp.egcn.fr"

    /** Pre-filled username shown when no connection has been saved yet. */
    const val DEFAULT_USERNAME = "tec"

    private const val PREFS_FILE_NAME = "pointex_kiosk_secure_prefs"
    private const val KEY_HOST = "sftp_host"
    private const val KEY_USERNAME = "ftp_username"
    private const val KEY_PASSWORD = "ftp_password"

    /** True once SFTP credentials have been saved. */
    fun isSaved(context: Context): Boolean =
        SecurePrefs.get(context, PREFS_FILE_NAME).contains(KEY_USERNAME)

    /** Saves [host], [username] and [password] for future SFTP connections. */
    fun save(context: Context, host: String, username: String, password: String) {
        SecurePrefs.get(context, PREFS_FILE_NAME).edit()
            .putString(KEY_HOST, host)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    /** Returns the saved connection details, or null if none is stored. */
    fun credentials(context: Context): SftpCredentials? {
        val prefs = SecurePrefs.get(context, PREFS_FILE_NAME)
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        val host = prefs.getString(KEY_HOST, null) ?: DEFAULT_HOST
        return SftpCredentials(host, username, password)
    }

    /** Clears the saved connection details (e.g. after a login failure). */
    fun clear(context: Context) {
        SecurePrefs.get(context, PREFS_FILE_NAME).edit()
            .remove(KEY_HOST)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }
}
