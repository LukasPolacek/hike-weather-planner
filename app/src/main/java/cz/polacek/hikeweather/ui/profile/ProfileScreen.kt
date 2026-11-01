package cz.polacek.hikeweather.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.polacek.hikeweather.BuildConfig
import cz.polacek.hikeweather.R
import cz.polacek.hikeweather.feature.alerts.Notifications
import cz.polacek.hikeweather.feature.tracking.TrackingService
import cz.polacek.hikeweather.ui.components.Formatters
import kotlinx.coroutines.launch

/** Profil: jmeno, statistiky, zaloha do cloudu, upozorneni, sledovani vyletu a smazani dat */
@Composable
fun ProfileScreen(
    onDataDeleted: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val lastBackup by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val alertsEnabled by viewModel.alertsEnabled.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val tracking by TrackingService.isRunning.collectAsStateWithLifecycle()

    var editName by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val permissionText = stringResource(R.string.permission_needed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.BackupDone -> snackbar.showSnackbar(context.getString(R.string.backup_done, event.count))
                is ProfileEvent.RestoreDone -> snackbar.showSnackbar(context.getString(R.string.restore_done, event.count))
                is ProfileEvent.Failed -> snackbar.showSnackbar(context.getString(R.string.backup_failed, event.message))
                ProfileEvent.SignInFailed -> snackbar.showSnackbar(context.getString(R.string.sign_in_failed))
                ProfileEvent.DataDeleted -> onDataDeleted()
            }
        }
    }

    fun granted(permission: String) =
        (permission == Manifest.permission.POST_NOTIFICATIONS && !Notifications.isPermissionRequired()) ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // upozorneni potrebuji povoleni notifikaci
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) viewModel.setAlertsEnabled(true) else scope.launch { snackbar.showSnackbar(permissionText) }
    }
    // sledovani potrebuje polohu (a notifikace pro jeji zobrazeni)
    val trackingLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val location = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || granted(Manifest.permission.ACCESS_FINE_LOCATION)
        if (location) TrackingService.start(context) else scope.launch { snackbar.showSnackbar(permissionText) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfileHeader(userName.orEmpty(), stats, onEdit = { editName = true })

            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            // zaloha
            SettingsCard(stringResource(R.string.backup_section)) {
                val currentAccount = account
                if (currentAccount == null) {
                    Text(stringResource(R.string.backup_text), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { viewModel.signIn(context) }, enabled = !busy) {
                        Text(stringResource(R.string.sign_in_google))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(currentAccount.name ?: currentAccount.email.orEmpty(), style = MaterialTheme.typography.titleSmall)
                            currentAccount.email?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = viewModel::signOut, enabled = !busy) {
                            Icon(Icons.AutoMirrored.Filled.Logout, stringResource(R.string.sign_out))
                        }
                    }
                    Text(
                        lastBackup?.let { stringResource(R.string.last_backup, Formatters.dateTime(it)) }
                            ?: stringResource(R.string.never_backed_up),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::backup, enabled = !busy, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.CloudUpload, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_now))
                        }
                        OutlinedButton(onClick = viewModel::restore, enabled = !busy, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.CloudDownload, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.restore))
                        }
                    }
                }
            }

            // upozorneni na pocasi
            SettingsCard(stringResource(R.string.alerts_section)) {
                SwitchRow(stringResource(R.string.alerts_switch), alertsEnabled) { enabled ->
                    if (enabled && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setAlertsEnabled(enabled)
                    }
                }
                Text(stringResource(R.string.alerts_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // sledovani vyletu
            SettingsCard(stringResource(R.string.tracking_section)) {
                SwitchRow(stringResource(R.string.tracking_switch), tracking) { enabled ->
                    if (!enabled) {
                        TrackingService.stop(context)
                    } else {
                        trackingLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        )
                    }
                }
                Text(stringResource(R.string.tracking_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // data
            SettingsCard(stringResource(R.string.data_section)) {
                OutlinedButton(onClick = { confirmDelete = true }, enabled = !busy) {
                    Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error)
                }
            }

            Text(
                stringResource(R.string.version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (editName) {
        var value by remember { mutableStateOf(userName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(stringResource(R.string.edit_name)) },
            text = {
                OutlinedTextField(value, { value = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.rename(value); editName = false }, enabled = value.isNotBlank()) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_all_title)) },
            text = { Text(stringResource(R.string.delete_all_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    TrackingService.stop(context)
                    viewModel.deleteAllData()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ProfileHeader(name: String, stats: TripStats, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${Formatters.plural(stats.total, "výlet", "výlety", "výletů")} · " +
                    Formatters.plural(stats.upcoming, "nadcházející", "nadcházející", "nadcházejících"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, stringResource(R.string.edit_name)) }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
