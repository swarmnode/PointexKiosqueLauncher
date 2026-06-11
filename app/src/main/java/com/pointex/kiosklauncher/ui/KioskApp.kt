package com.pointex.kiosklauncher.ui

import android.content.Intent
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.pointex.kiosklauncher.data.PinRepository

private enum class KioskScreen { PROVISIONING_REQUIRED, PIN_SETUP, HOME, FTP_INSTALL }

/**
 * Single-screen, state-driven root composable.
 *
 * On first run, [ProvisioningRequiredScreen] blocks setup until the app is
 * confirmed as Device Owner (`KioskPolicyManager.isDeviceOwner`). Once
 * provisioned, shows [PinSetupScreen] until an administrator PIN exists,
 * then [HomeScreen]. A discreet long-press opens [AdminPinDialog]; once the
 * PIN is verified, [AdminMenuDialog] lets the administrator open system
 * Settings, Wi-Fi settings (e.g. to set a static IP) or SIM card settings
 * (lock-task mode is released first via [openSystemSettings]), or manage
 * Pointex apps via [FtpInstallScreen]. [activity]'s `onResume` re-applies
 * lock-task mode automatically when the kiosk regains focus.
 */
@Composable
fun KioskApp(activity: ComponentActivity, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var screen by remember {
        mutableStateOf(
            when {
                !KioskPolicyManager.isDeviceOwner(context) && !PinRepository.isPinSet(context) ->
                    KioskScreen.PROVISIONING_REQUIRED

                PinRepository.isPinSet(context) -> KioskScreen.HOME
                else -> KioskScreen.PIN_SETUP
            }
        )
    }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showAdminMenu by remember { mutableStateOf(false) }
    var showRebootConfirm by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf(KioskAppRepository.getAllowedApps(context)) }

    LaunchedEffect(apps) {
        KioskPolicyManager.updateLockTaskPackages(context, apps.map { it.packageName })
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
                        screen = KioskScreen.PIN_SETUP
                    } else {
                        Toast.makeText(context, "Toujours non configuré en mode kiosque", Toast.LENGTH_SHORT).show()
                    }
                },
                onContinueAnyway = { screen = KioskScreen.PIN_SETUP },
            )

            KioskScreen.PIN_SETUP -> PinSetupScreen(
                onPinConfirmed = { pin ->
                    PinRepository.setPin(context, pin)
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
            onRebootRequested = {
                showAdminMenu = false
                showRebootConfirm = true
            },
        )
    }

    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text("Redémarrer l'appareil") },
            text = { Text("Voulez-vous vraiment redémarrer le terminal maintenant ?") },
            confirmButton = {
                TextButton(onClick = {
                    showRebootConfirm = false
                    if (!KioskPolicyManager.rebootDevice(context)) {
                        Toast.makeText(context, "Redémarrage impossible pour le moment", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Redémarrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) {
                    Text("Annuler")
                }
            },
        )
    }
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
