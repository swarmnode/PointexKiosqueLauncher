package com.pointex.kiosklauncher.data

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import com.pointex.kiosklauncher.admin.KioskPolicyManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume

/**
 * Installs and uninstalls APK files using [PackageInstaller]. When Pointex
 * Kiosk Launcher is the device owner, both operations are silent (no
 * confirmation dialog). On a non-device-owner device (limited kiosk mode),
 * the system replies with `STATUS_PENDING_USER_ACTION` instead: the kiosk is
 * unpinned and the system confirmation activity is launched (see
 * [launchUserConfirmation]).
 */
object ApkInstaller {

    private const val ACTION_INSTALL_RESULT = "com.pointex.kiosklauncher.ACTION_INSTALL_RESULT"
    private const val ACTION_UNINSTALL_RESULT = "com.pointex.kiosklauncher.ACTION_UNINSTALL_RESULT"

    /**
     * Streams [apkFile] into a new install session and commits it. Resumes
     * with `true` and a French success message on success, or `false` and an
     * error message otherwise. Must be called from a coroutine.
     */
    suspend fun install(context: Context, apkFile: File): Pair<Boolean, String> =
        suspendCancellableCoroutine { continuation ->
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

            // Allow installing an older versionCode over a newer one. Only
            // honoured for Device Owner; otherwise the system still blocks the
            // downgrade (handled with a clear message below) and the technician
            // must uninstall first.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    PackageInstaller.SessionParams::class.java
                        .getMethod("setRequestDowngrade", Boolean::class.javaPrimitiveType)
                        .invoke(params, true)
                } catch (_: Exception) {
                    // Hidden API unavailable on this device; ignore.
                }
            }

            val sessionId: Int
            try {
                sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)
                session.use { s ->
                    s.openWrite("apk", 0, apkFile.length()).use { out ->
                        FileInputStream(apkFile).use { input -> input.copyTo(out) }
                        s.fsync(out)
                    }

                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(receiverContext: Context, intent: Intent) {
                            val status = intent.getIntExtra(
                                PackageInstaller.EXTRA_STATUS,
                                PackageInstaller.STATUS_FAILURE
                            )

                            // On a non-device-owner device, the system requires
                            // user confirmation: relaunch the confirmation intent
                            // and keep listening for the final SUCCESS/FAILURE
                            // broadcast it triggers.
                            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                                val confirmIntent = confirmIntentOf(intent)
                                if (confirmIntent != null) {
                                    launchUserConfirmation(context, confirmIntent)
                                    return
                                }
                            }

                            receiverContext.unregisterReceiver(this)
                            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            if (continuation.isActive) {
                                continuation.resume(
                                    if (status == PackageInstaller.STATUS_SUCCESS) {
                                        true to "Installation réussie"
                                    } else {
                                        false to friendlyInstallError(message)
                                    }
                                )
                            }
                        }
                    }

                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(ACTION_INSTALL_RESULT),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )

                    continuation.invokeOnCancellation {
                        runCatching { context.unregisterReceiver(receiver) }
                    }

                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        sessionId,
                        Intent(ACTION_INSTALL_RESULT).setPackage(context.packageName),
                        flags
                    )

                    s.commit(pendingIntent.intentSender)
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(false to (e.message ?: "Échec de l'installation"))
                }
            }
        }

    /**
     * Uninstalls [packageName] via [PackageInstaller]. Silent (no confirmation
     * dialog) when the app is device owner; otherwise the system confirmation
     * flow is handled like in [install]. Resumes with `true` and a French
     * success message on success, or `false` and an error message otherwise.
     * Must be called from a coroutine.
     */
    suspend fun uninstall(context: Context, packageName: String): Pair<Boolean, String> =
        suspendCancellableCoroutine { continuation ->
            val packageInstaller = context.packageManager.packageInstaller

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    val status = intent.getIntExtra(
                        PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE
                    )

                    // Same non-device-owner confirmation flow as install().
                    if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                        val confirmIntent = confirmIntentOf(intent)
                        if (confirmIntent != null) {
                            launchUserConfirmation(context, confirmIntent)
                            return
                        }
                    }

                    receiverContext.unregisterReceiver(this)
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    if (continuation.isActive) {
                        continuation.resume(
                            if (status == PackageInstaller.STATUS_SUCCESS) {
                                true to "Désinstallation réussie"
                            } else {
                                false to (message?.takeIf { it.isNotBlank() } ?: "Échec de la désinstallation")
                            }
                        )
                    }
                }
            }

            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(ACTION_UNINSTALL_RESULT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            continuation.invokeOnCancellation {
                runCatching { context.unregisterReceiver(receiver) }
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                packageName.hashCode(),
                Intent(ACTION_UNINSTALL_RESULT).setPackage(context.packageName),
                flags
            )

            try {
                packageInstaller.uninstall(packageName, pendingIntent.intentSender)
            } catch (e: Exception) {
                context.unregisterReceiver(receiver)
                if (continuation.isActive) {
                    continuation.resume(false to (e.message ?: "Échec de la désinstallation"))
                }
            }
        }

    /** Maps a raw PackageInstaller status message to a French, user-facing message. */
    private fun friendlyInstallError(message: String?): String = when {
        message == null -> "Échec de l'installation"
        message.contains("DOWNGRADE", ignoreCase = true) ->
            "Version plus ancienne que celle installée. Pour revenir en arrière, " +
                "désinstallez d'abord l'application (les données seront perdues)."
        message.contains("INCONSISTENT", ignoreCase = true) ||
            message.contains("signature", ignoreCase = true) ->
            "Signature incompatible avec la version installée. Désinstallez d'abord " +
                "l'application avant d'installer celle-ci."
        message.isBlank() -> "Échec de l'installation"
        else -> message
    }

    /** Extracts the system confirmation intent from a `STATUS_PENDING_USER_ACTION` broadcast. */
    private fun confirmIntentOf(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }

    /**
     * Launches the system install/uninstall confirmation activity. In limited
     * kiosk mode the app is pinned via screen pinning, which blocks launching
     * other apps' activities, so the kiosk is unpinned first; MainActivity
     * re-pins on its next resume, once the confirmation dialog is dismissed.
     */
    private fun launchUserConfirmation(context: Context, confirmIntent: Intent) {
        context.findActivity()?.let { KioskPolicyManager.exitLockTask(it) }
        context.startActivity(confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
