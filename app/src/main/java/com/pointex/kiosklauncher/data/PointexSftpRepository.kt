package com.pointex.kiosklauncher.data

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.SftpException
import java.io.File
import java.io.FileOutputStream

/** A Pointex app available for download from the SFTP server. */
data class PointexApp(
    val displayName: String,
    val remotePath: String,
    val sizeBytes: Long,
)

/** Result of an SFTP operation: either [Success] or [Failure] with a French, user-facing [message]. */
sealed class FtpResult<out T> {
    data class Success<T>(val value: T) : FtpResult<T>()
    data class Failure(val message: String) : FtpResult<Nothing>()
}

/**
 * Browses and downloads Pointex Android apps published on EGCN's SFTP server.
 * All functions perform blocking network I/O and must be called off the main thread.
 */
object PointexSftpRepository {

    private const val PORT = 22
    private const val BASE_PATH = "Versions-NF/Android/"
    private const val CONNECT_TIMEOUT_MS = 10_000

    /** Connects, authenticates and lists the Pointex apps available for installation. */
    fun listApps(host: String, username: String, password: String): FtpResult<List<PointexApp>> =
        withChannel(host, username, password) { channel ->
            val topLevel = try {
                channel.ls(BASE_PATH).filterIsInstance<ChannelSftp.LsEntry>()
            } catch (e: SftpException) {
                return@withChannel FtpResult.Failure("Impossible de lister le dossier distant")
            }

            val apps = mutableListOf<PointexApp>()
            for (entry in topLevel) {
                if (entry.filename == "." || entry.filename == "..") continue
                val entryPath = BASE_PATH + entry.filename
                when {
                    entry.attrs.isDir -> {
                        val apk = findLatestApk(channel, entryPath)
                        if (apk != null) {
                            apps.add(PointexApp(entry.filename, "$entryPath/${apk.filename}", apk.attrs.size))
                        }
                    }

                    entry.filename.endsWith(".apk", ignoreCase = true) -> {
                        apps.add(PointexApp(entry.filename.removeSuffix(".apk"), entryPath, entry.attrs.size))
                    }
                }
            }
            FtpResult.Success(apps.sortedBy { it.displayName.lowercase() })
        }

    /** Downloads [app] into [destFile], overwriting it if it already exists. */
    fun download(host: String, username: String, password: String, app: PointexApp, destFile: File): FtpResult<Unit> =
        withChannel(host, username, password) { channel ->
            try {
                FileOutputStream(destFile).use { out -> channel.get(app.remotePath, out) }
                FtpResult.Success(Unit)
            } catch (e: SftpException) {
                FtpResult.Failure("Échec du téléchargement de \"${app.displayName}\"")
            }
        }

    /** Picks the most recently modified `.apk` file directly inside [dirPath], or null if none. */
    private fun findLatestApk(channel: ChannelSftp, dirPath: String): ChannelSftp.LsEntry? =
        try {
            channel.ls(dirPath)
                .filterIsInstance<ChannelSftp.LsEntry>()
                .filter { !it.attrs.isDir && it.filename.endsWith(".apk", ignoreCase = true) }
                .maxByOrNull { it.attrs.mTime }
        } catch (e: SftpException) {
            null
        }

    /** Opens an SFTP session, authenticates, runs [block], then always disconnects. */
    private fun <T> withChannel(host: String, username: String, password: String, block: (ChannelSftp) -> FtpResult<T>): FtpResult<T> {
        val session = try {
            JSch().getSession(username, host, PORT).apply {
                setPassword(password.toByteArray())
                // No known_hosts database is bundled with the app, so the
                // server's host key cannot be verified against a trusted
                // fingerprint. The connection is still encrypted.
                setConfig("StrictHostKeyChecking", "no")
                connect(CONNECT_TIMEOUT_MS)
            }
        } catch (e: JSchException) {
            return FtpResult.Failure(connectFailureMessage(e))
        }

        return try {
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(CONNECT_TIMEOUT_MS)
            try {
                block(channel)
            } finally {
                channel.disconnect()
            }
        } catch (e: Exception) {
            FtpResult.Failure(e.message ?: "Erreur de connexion SFTP")
        } finally {
            session.disconnect()
        }
    }

    private fun connectFailureMessage(e: JSchException): String =
        if (e.message?.contains("Auth", ignoreCase = true) == true) {
            "Identifiant ou mot de passe incorrect"
        } else {
            "Connexion refusée par le serveur SFTP"
        }
}
