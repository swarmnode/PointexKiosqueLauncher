package com.pointex.kiosklauncher.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pointex.kiosklauncher.R
import com.pointex.kiosklauncher.admin.KioskPolicyManager
import com.pointex.kiosklauncher.data.KioskAppRepository
import com.pointex.kiosklauncher.data.KioskModeRepository
import com.pointex.kiosklauncher.data.WatchdogRepository

private enum class KioskScreen { PROVISIONING_REQUIRED, HOME, FTP_INSTALL }

/**
 * Single-screen, state-driven root composable.
 *
 * On first run, [ProvisioningRequiredScreen] blocks setup until the app is
 * confirmed as Device Owner (`KioskPolicyManager.isDeviceOwner`) or the
 * administrator opts into limited kiosk mode, then shows [HomeScreen]. A
 * discreet long-press opens [AdminPinDialog] (challenge-response code, no
 * stored PIN); once verified, [AdminMenuDialog] lets the administrator open
 * system Settings, Wi-Fi settings (e.g. to set a static IP) or SIM card
 * settings (lock-task mode is released first via [openSystemSettings]), or
 * manage Pointex apps via [FtpInstallScreen]. [activity]'s `onResume`
 * re-applies lock-task mode automatically when the kiosk regains focus.
 */
@Composable
fun KioskApp(activity: ComponentActivity, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var screen by remember {
        mutableStateOf(
            if (KioskPolicyManager.isDeviceOwner(context) || KioskModeRepository.isLimitedModeChosen(context)) {
                KioskScreen.HOME
            } else {
                KioskScreen.PROVISIONING_REQUIRED
            }
        )
    }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showAdminMenu by remember { mutableStateOf(false) }
    var accessScope by remember { mutableStateOf(WatchdogRepository.scope(context)) }
    var apps by remember { mutableStateOf(KioskAppRepository.getAllowedApps(context)) }

    val homeRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Refusal is fine: the technician can still set the default launcher in Settings. */ }

    fun requestHomeRole() {
        homeRoleRequestIntent(context)?.let { homeRoleLauncher.launch(it) }
    }

    // Limited-mode setup launches the device-admin activation screen first
    // (blocks uninstall), then chains to the default-launcher role request.
    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { requestHomeRole() }



    LaunchedEffect(apps) {
        val packages = apps.map { it.packageName }
        KioskPolicyManager.updateLockTaskPackages(context, packages)
        WatchdogRepository.allowedPackages = packages.toSet()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                apps = KioskAppRepository.getAllowedApps(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Image(
        painter = painterResource(R.drawable.wallpaper_background),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        when (screen) {
            KioskScreen.PROVISIONING_REQUIRED -> ProvisioningRequiredScreen(
                onRetry = {
                    if (KioskPolicyManager.isDeviceOwner(context)) {
                        screen = KioskScreen.HOME
                    } else {
                        Toast.makeText(context, "Toujours non configuré en mode kiosque", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenSecuritySettings = {
                    openSystemSettings(activity, context, Settings.ACTION_SECURITY_SETTINGS)
                },
                onContinueAnyway = {
                    // Persisted: granting the HOME role recreates the app,
                    // which must not land back on the provisioning screen.
                    KioskModeRepository.setLimitedModeChosen(context)
                    val adminIntent = KioskPolicyManager.addAdminIntent(context)
                    if (adminIntent != null) {
                        adminLauncher.launch(adminIntent) // requestHomeRole() on return
                    } else {
                        requestHomeRole()
                    }
                    screen = KioskScreen.HOME
                },
            )

            KioskScreen.HOME -> HomeScreen(
                apps = apps,
                onAppClick = { app -> context.startActivity(KioskAppRepository.launchIntentFor(app)) },
                onAdminRequested = { showAdminDialog = true },
                onInstallRequested = { screen = KioskScreen.FTP_INSTALL },
            )

            KioskScreen.FTP_INSTALL -> FtpInstallScreen(
                onClose = { screen = KioskScreen.HOME },
                onInstalled = {
                    apps = KioskAppRepository.getAllowedApps(context)
                    screen = KioskScreen.HOME
                },
            )
        }
    }

    if (showAdminDialog) {
        AdminPinDialog(
            onDismiss = { showAdminDialog = false },
            onPinVerified = {
                showAdminDialog = false
                showAdminMenu = true
            },
        )
    }

    if (showAdminMenu) {
        AdminMenuDialog(
            onDismiss = { showAdminMenu = false },
            onOpenSettings = {
                showAdminMenu = false
                openSystemSettings(activity, context, Settings.ACTION_SETTINGS)
            },
            onOpenWifiSettings = {
                showAdminMenu = false
                openSystemSettings(activity, context, Settings.ACTION_WIFI_SETTINGS)
            },
            onOpenSimSettings = {
                showAdminMenu = false
                if (hasSimCard(context)) {
                    openSystemSettings(activity, context, Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                } else {
                    Toast.makeText(context, "Aucune carte SIM détectée", Toast.LENGTH_SHORT).show()
                }
            },
            onManageApps = {
                showAdminMenu = false
                screen = KioskScreen.FTP_INSTALL
            },
            accessGuardLabel = accessGuardLabel(accessScope),
            onCycleAccessGuard = {
                val next = when (accessScope) {
                    WatchdogRepository.Scope.DISABLED -> WatchdogRepository.Scope.SETTINGS_ONLY
                    WatchdogRepository.Scope.SETTINGS_ONLY -> WatchdogRepository.Scope.ALL_NON_ALLOWED
                    WatchdogRepository.Scope.ALL_NON_ALLOWED -> WatchdogRepository.Scope.DISABLED
                }
                WatchdogRepository.setScope(context, next)
                accessScope = next
                Toast.makeText(context, "Protection d'accès : ${accessGuardLabel(next)}", Toast.LENGTH_SHORT).show()
            },
            onEnableAccessGuard = {
                showAdminMenu = false
                openSystemSettings(activity, context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
            },
        )
    }
}

private fun accessGuardLabel(scope: WatchdogRepository.Scope): String = when (scope) {
    WatchdogRepository.Scope.DISABLED -> "désactivée"
    WatchdogRepository.Scope.SETTINGS_ONLY -> "Paramètres seulement"
    WatchdogRepository.Scope.ALL_NON_ALLOWED -> "toutes les apps"
}

/**
 * Releases lock-task mode (temporarily allowing [com.android.settings] in the
 * lock-task allowlist, see [KioskPolicyManager.exitLockTask]) and opens the
 * given Settings screen. [KioskPolicyManager.enterLockTask] re-locks the
 * kiosk on the next `onResume`.
 */
private fun openSystemSettings(activity: ComponentActivity, context: android.content.Context, action: String) {
    KioskPolicyManager.exitLockTask(activity)
    context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Builds the system dialog intent requesting the default-launcher (HOME)
 * role, or null when it cannot or need not be requested (pre-Android 10,
 * role unavailable, or already held). Used when entering limited kiosk mode,
 * where Device Owner's `addPersistentPreferredActivity` is a no-op: holding
 * the HOME role keeps the Home button returning to the kiosk instead.
 */
private fun homeRoleRequestIntent(context: android.content.Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
    if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME) || roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
        return null
    }
    return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
}

/**
 * True if at least one SIM slot reports a card present. [Settings.ACTION_NETWORK_OPERATOR_SETTINGS]
 * closes itself instantly when no SIM is inserted, which looks like a broken button.
 */
private fun hasSimCard(context: android.content.Context): Boolean {
    val telephonyManager = context.getSystemService(TelephonyManager::class.java) ?: return false
    return (0..1).any { slot ->
        val state = telephonyManager.getSimState(slot)
        state != TelephonyManager.SIM_STATE_ABSENT && state != TelephonyManager.SIM_STATE_UNKNOWN
    }
}
