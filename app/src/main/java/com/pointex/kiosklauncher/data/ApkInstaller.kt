package com.pointex.kiosklauncher.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume

/**
 * Installs APK files using [PackageInstaller]. Because Pointex Kiosk Launcher
 * is the device owner, the install session is committed without showing the
 * usual "Do you want to install this app?" confirmation dialog.
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
                            receiverContext.unregisterReceiver(this)
                            val status = intent.getIntExtra(
                                PackageInstaller.EXTRA_STATUS,
                                PackageInstaller.STATUS_FAILURE
                            )
                            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            if (continuation.isActive) {
                                continuation.resume(
                                    if (status == PackageInstaller.STATUS_SUCCESS) {
                                        true to "Installation réussie"
                                    } else {
                                        false to (message?.takeIf { it.isNotBlank() } ?: "Échec de l'installation")
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
     * Uninstalls [packageName] via [PackageInstaller]. Because Pointex Kiosk
     * Launcher is the device owner, this is silent (no confirmation dialog).
     * Resumes with `true` and a French success message on success, or `false`
     * and an error message otherwise. Must be called from a coroutine.
     */
    suspend fun uninstall(context: Context, packageName: String): Pair<Boolean, String> =
        suspendCancellableCoroutine { continuation ->
            val packageInstaller = context.packageManager.packageInstaller

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    receiverContext.unregisterReceiver(this)
                    val status = intent.getIntExtra(
                        PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE
                    )
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
}
