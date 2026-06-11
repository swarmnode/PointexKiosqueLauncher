package com.pointex.kiosklauncher.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pointex.kiosklauncher.data.ApkInstaller
import com.pointex.kiosklauncher.data.FtpCredentialsRepository
import com.pointex.kiosklauncher.data.FtpResult
import com.pointex.kiosklauncher.data.KioskApp
import com.pointex.kiosklauncher.data.KioskAppRepository
import com.pointex.kiosklauncher.data.PointexApp
import com.pointex.kiosklauncher.data.PointexSftpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class FtpStep { CREDENTIALS, LOADING, LIST, INSTALLING, RESULT }

/**
 * First-run helper screen: lets the kiosk administrator log in to the EGCN
 * SFTP server, pick a Pointex app from `/Versions-NF/Android/`, download it
 * and install it silently (this app is the device owner). Credentials are
 * saved via [FtpCredentialsRepository] for later use.
 */
@Composable
fun FtpInstallScreen(
    onClose: () -> Unit,
    onInstalled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedCredentials = remember { FtpCredentialsRepository.credentials(context) }
    var host by remember { mutableStateOf(savedCredentials?.host ?: FtpCredentialsRepository.DEFAULT_HOST) }
    var username by remember { mutableStateOf(savedCredentials?.username ?: FtpCredentialsRepository.DEFAULT_USERNAME) }
    var password by remember { mutableStateOf(savedCredentials?.password ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(if (savedCredentials != null) FtpStep.LOADING else FtpStep.CREDENTIALS) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var apps by remember { mutableStateOf<List<PointexApp>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<PointexApp?>(null) }
    var resultSuccess by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf(KioskAppRepository.getAllowedApps(context)) }
    var appPendingUninstall by remember { mutableStateOf<KioskApp?>(null) }

    suspend fun loadApps() {
        step = FtpStep.LOADING
        errorMessage = null
        when (val result = withContext(Dispatchers.IO) { PointexSftpRepository.listApps(host, username, password) }) {
            is FtpResult.Success -> {
                FtpCredentialsRepository.save(context, host, username, password)
                apps = result.value
                selectedApp = apps.firstOrNull()
                step = FtpStep.LIST
            }

            is FtpResult.Failure -> {
                errorMessage = result.message
                step = FtpStep.CREDENTIALS
            }
        }
    }

    suspend fun downloadAndInstall(app: PointexApp) {
        step = FtpStep.INSTALLING
        val destFile = File(context.cacheDir, "pointex_install.apk")
        when (val downloadResult = withContext(Dispatchers.IO) {
            PointexSftpRepository.download(host, username, password, app, destFile)
        }) {
            is FtpResult.Success -> {
                val (success, message) = ApkInstaller.install(context, destFile)
                destFile.delete()
                resultSuccess = success
                resultMessage = message
                step = FtpStep.RESULT
            }

            is FtpResult.Failure -> {
                resultSuccess = false
                resultMessage = downloadResult.message
                step = FtpStep.RESULT
            }
        }
    }

    suspend fun uninstallApp(app: KioskApp) {
        val (_, message) = ApkInstaller.uninstall(context, app.packageName)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        installedApps = KioskAppRepository.getAllowedApps(context)
    }

    LaunchedEffect(Unit) {
        if (savedCredentials != null) loadApps()
    }

    when (step) {
        FtpStep.CREDENTIALS -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Installer une application Pointex",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Connectez-vous au serveur de mise à jour pour choisir une application à installer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Adresse du serveur") },
                singleLine = true,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Identifiant SFTP") },
                singleLine = true,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                        )
                    }
                },
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )

            Button(
                onClick = { scope.launch { loadApps() } },
                enabled = host.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            ) {
                Text("Se connecter")
            }

            TextButton(onClick = onClose, modifier = Modifier.padding(top = 8.dp)) {
                Text("Plus tard")
            }
        }

        FtpStep.LOADING -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text(
                text = "Connexion au serveur...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        FtpStep.LIST -> Column(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp),
        ) {
            if (installedApps.isNotEmpty()) {
                Text(
                    text = "Applications installées",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                installedApps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = app.label, modifier = Modifier.weight(1f))
                        TextButton(onClick = { appPendingUninstall = app }) {
                            Text("Désinstaller")
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            Text(
                text = "Choisissez une application Pointex",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (apps.isEmpty()) {
                Text(
                    text = "Aucune application disponible sur le serveur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(apps, key = { it.remotePath }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedApp = app }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selectedApp == app, onClick = { selectedApp = app })
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = app.displayName, fontWeight = FontWeight.Bold)
                                if (app.sizeBytes > 0) {
                                    Text(
                                        text = "${app.sizeBytes / (1024 * 1024)} Mo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClose) {
                    Text("Plus tard")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { selectedApp?.let { app -> scope.launch { downloadAndInstall(app) } } },
                    enabled = selectedApp != null,
                ) {
                    Text("Télécharger et installer")
                }
            }
        }

        FtpStep.INSTALLING -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text(
                text = "Téléchargement et installation en cours...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        FtpStep.RESULT -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (resultSuccess) "Installation réussie" else "Échec de l'installation",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (resultSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                text = resultMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!resultSuccess) {
                    TextButton(onClick = { step = FtpStep.LIST }) {
                        Text("Réessayer")
                    }
                }
                Button(onClick = { if (resultSuccess) onInstalled() else onClose() }) {
                    Text(if (resultSuccess) "Terminer" else "Fermer")
                }
            }
        }
    }

    appPendingUninstall?.let { app ->
        AlertDialog(
            onDismissRequest = { appPendingUninstall = null },
            title = { Text("Désinstaller ${app.label} ?") },
            text = { Text("Cette application sera supprimée de l'appareil.") },
            confirmButton = {
                TextButton(onClick = {
                    appPendingUninstall = null
                    scope.launch { uninstallApp(app) }
                }) {
                    Text("Désinstaller")
                }
            },
            dismissButton = {
                TextButton(onClick = { appPendingUninstall = null }) {
                    Text("Annuler")
                }
            },
        )
    }
}
