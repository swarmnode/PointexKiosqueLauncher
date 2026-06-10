package com.pointex.kiosklauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.Locale

/**
 * Resolves the set of installed applications that are allowed inside the
 * kiosk: only packages whose package name contains "pointex" or "fiducial".
 */
object KioskAppRepository {

    private val ALLOWED_PACKAGE_KEYWORDS = listOf("pointex", "fiducial")

    /**
     * Returns every launchable app whose package name matches one of the
     * allowed keywords, sorted alphabetically by display label.
     */
    fun getAllowedApps(context: Context): List<KioskApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Note: deliberately not using MATCH_DEFAULT_ONLY — some Pointex apps'
        // launcher activities declare CATEGORY_LAUNCHER without CATEGORY_DEFAULT,
        // which that flag would silently exclude.
        val resolvedActivities = packageManager.queryIntentActivities(launcherIntent, 0)

        return resolvedActivities
            .asSequence()
            .filter { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName.lowercase(Locale.ROOT)
                packageName != context.packageName &&
                    ALLOWED_PACKAGE_KEYWORDS.any { keyword -> packageName.contains(keyword) }
            }
            .map { resolveInfo ->
                KioskApp(
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    icon = resolveInfo.loadIcon(packageManager),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
            .toList()
    }

    /** Builds the launch intent for the exact activity resolved for [app]. */
    fun launchIntentFor(app: KioskApp): Intent =
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
}
