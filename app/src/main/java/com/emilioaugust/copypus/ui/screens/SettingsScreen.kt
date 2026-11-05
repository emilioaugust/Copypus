package com.emilioaugust.copypus.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emilioaugust.copypus.BuildConfig
import com.emilioaugust.copypus.R
import com.emilioaugust.copypus.data.enums.AutoDeleteOption
import com.emilioaugust.copypus.data.enums.AppLanguage
import com.emilioaugust.copypus.data.enums.PauseDuration
import com.emilioaugust.copypus.data.viewmodel.SettingsViewModel
import com.emilioaugust.copypus.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.emilioaugust.copypus.data.backup.BackupException
import com.emilioaugust.copypus.data.backup.BackupManager
import com.emilioaugust.copypus.service.ServiceLocator
import com.emilioaugust.copypus.utils.checkNotificationEnabled
import com.emilioaugust.copypus.utils.isAccessibilityServiceEnabled
import com.emilioaugust.copypus.utils.isBatteryOptimizationIgnored
import com.emilioaugust.copypus.utils.isOverlayPermissionGranted
import com.emilioaugust.copypus.utils.isPostNotificationsGranted
import com.emilioaugust.copypus.utils.requestIgnoreBatteryOptimization
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateToSnippets: () -> Unit) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val autoDelete by viewModel.autoDelete.collectAsState(initial = AutoDeleteOption.NEVER)
    val language by viewModel.language.collectAsState(initial = AppLanguage.ENGLISH)
    val pauseDuration by viewModel.pauseDuration.collectAsState(initial = PauseDuration.MIN_15)
    val backupManager = remember {
        BackupManager(
            context.applicationContext,
            ServiceLocator.repository
        )
    }

    var isProcessing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }

    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }
    var pendingImportPassword by remember { mutableStateOf<CharArray?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val password = pendingExportPassword
        pendingExportPassword = null

        if (uri == null || password == null) {
            password?.fill('\u0000')
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            isProcessing = true
            try {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Cannot open the destination file")

                output.use {
                    withContext(Dispatchers.IO) {
                        backupManager.exportBackup(it, password)
                    }
                }

                Toast.makeText(
                    context,
                    context.getString(R.string.backup_saved),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.export_failed, e.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()

                Log.d("Export", e.message.toString())
            } finally {
                isProcessing = false
                password.fill('\u0000')
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val password = pendingImportPassword
        pendingImportPassword = null

        if (uri == null || password == null) {
            password?.fill('\u0000')
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            isProcessing = true
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Cannot open the selected file")

                val result = input.use {
                    withContext(Dispatchers.IO) {
                        backupManager.importBackup(it, password)
                    }
                }

                Toast.makeText(
                    context,
                    context.getString(R.string.restored_items, result.importedItems),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: BackupException.WrongPassword) {
                Toast.makeText(
                    context,
                    context.getString(R.string.wrong_password_or_corrupted_file),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.import_failed, e.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isProcessing = false
                password.fill('\u0000')
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.topbar_settings),
                    color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.background,
                    titleContentColor =
                        MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)) {
            item {

                // THEME MODE
                Text(text = stringResource(R.string.title_appearance_settings), style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults
                                .itemShape(
                                    index = index,
                                    count = ThemeMode.entries.size
                                )
                        ) {
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.system_theme)
                                    ThemeMode.LIGHT -> stringResource(R.string.light_theme)
                                    ThemeMode.DARK -> stringResource(R.string.dark_theme)
                                }
                            )
                        }
                    }
                }

                // SYSTEM
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = stringResource(R.string.system),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))

                SettingsCard {
                    SettingsSystemItem(
                        stringResource(R.string.accessibility_service),
                        stringResource(
                            R.string.allow_saving_the_clipboard_without_opening_the_app
                        ),
                        icon = if(isAccessibilityServiceEnabled(context)) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RemoveCircle
                        },
                        iconColor = if(isAccessibilityServiceEnabled(context)) {
                            Color(0xFF16AB46)
                        } else {
                            Color(0xFFFF2C2C)
                        },
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSystemItem(
                        stringResource(R.string.overlay_permission),
                        stringResource(
                            R.string.allow_it_to_save_copied_items_without_opening_the_app
                        ),
                        icon = if(isOverlayPermissionGranted(context)) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RemoveCircle
                        },
                        iconColor = if(isOverlayPermissionGranted(context)) {
                            Color(0xFF16AB46)
                        } else {
                            Color(0xFFFF2C2C)
                        },
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSystemItem(
                        stringResource(R.string.battery_optimization),
                        stringResource(
                            R.string.allow_copypus_to_ignore_battery_optimization
                        ),
                        icon = if(isBatteryOptimizationIgnored(context)) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RemoveCircle
                        },
                        iconColor = if(isBatteryOptimizationIgnored(context)) {
                            Color(0xFF16AB46)
                        } else {
                            Color(0xFFFF2C2C)
                        },
                        onClick = {
                            requestIgnoreBatteryOptimization(context)
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSystemItem(
                        stringResource(R.string.notification_access),
                        stringResource(R.string.allow_copypus_to_show_notifications),
                        icon = if(isPostNotificationsGranted(context)) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RemoveCircle
                        },
                        iconColor = if(isPostNotificationsGranted(context)) {
                            Color(0xFF16AB46)
                        } else {
                            Color(0xFFFF2C2C)
                        },
                        onClick = {
                            checkNotificationEnabled(activity)
                        }
                    )

                }

                // AUTO DELETE and PAUSE DURATION
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = stringResource(R.string.title_preferences_settings), style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                SettingsCard {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .clickable {
                            onNavigateToSnippets()
                        }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Snippets", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Save frequently used text and insert it with a tap", style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray, modifier = Modifier.width(230.dp))
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))

                    SettingsItem(
                        title = stringResource(R.string.auto_delete_title),
                        description = stringResource(R.string.auto_delete_text)
                    ) {
                        SettingsDropdown(
                            selected = autoDelete,
                            options = AutoDeleteOption.entries,
                            label = { stringResource(it.titleRes) },
                            onSelected = viewModel::setAutoDelete
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))

                    SettingsItem(
                        title = stringResource(R.string.pause_monitoring_title),
                        description = stringResource(R.string.choose_pause_duration_text)
                    ) {
                        SettingsDropdown(
                            selected = pauseDuration,
                            options = PauseDuration.entries,
                            label = { stringResource(it.titleRes) },
                            onSelected = viewModel::setPauseDuration
                        )
                    }

                }

                // LANGUAGE
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = stringResource(R.string.title_language_settings), style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                SettingsCard {
                    SettingsItem(
                        title = stringResource(R.string.language_title),
                        description = stringResource(R.string.language_text)
                    ) {
                        SettingsDropdown(
                            selected = language,
                            options = AppLanguage.entries,
                            label = { stringResource(it.titleRes) },
                            onSelected = { lang ->
                                scope.launch {
                                    viewModel.setLanguage(lang)
                                    activity.recreate()
                                }
                            }
                        )
                    }
                }

                // EXPORT or IMPORT
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = stringResource(R.string.backup),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                SettingsCard {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .clickable {
                            if (!isProcessing) {
                                showExportPasswordDialog = true
                            }
                        }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.export_your_data), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = stringResource(R.string.save_an_encrypted_backup_file), style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray, modifier = Modifier.width(230.dp))
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))

                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .clickable {
                            if (!isProcessing) {
                                showImportPasswordDialog = true
                            }
                        }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.import_data), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = stringResource(R.string.restore_from_a_backup_file), style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray, modifier = Modifier.width(230.dp))
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }


                // ABOUT
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = stringResource(R.string.title_about_settings),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                SettingsCard {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onTertiary)
                        Text(stringResource(R.string.app_version_title))
                        Spacer(modifier = Modifier.weight(1f))
                        Text("v${BuildConfig.VERSION_NAME}", modifier = Modifier.padding(end = 4.dp),
                            color = Color.Gray)
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))

                    AboutItem(
                        icon = Icons.Default.Feedback,
                        text = stringResource(R.string.app_feedback_support_title),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply { data = "mailto:emiliooaaugust@gmail.com".toUri() }
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))

                    AboutItem(
                        icon = Icons.Default.Code,
                        text = stringResource(R.string.view_source_code_title),
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/emilioaugust/Copypus".toUri()))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        if (showExportPasswordDialog) {
            PasswordDialog(
                title = stringResource(R.string.export_your_data),
                description = stringResource(R.string.create_a_password_to_protect_your_backup) +
                        stringResource(R.string.you_ll_need_it_to_restore_your_data_later) +
                        stringResource(R.string.the_password_can_t_be_recovered),
                confirmText = stringResource(R.string.export),
                requireConfirmation = true,
                onConfirm = { password ->
                    showExportPasswordDialog = false
                    pendingExportPassword = password

                    val fileName = "copypus_backup_${
                        SimpleDateFormat(
                            "yyyy-MM-dd_HH-mm",
                            Locale.getDefault()
                        ).format(Date())
                    }.copypus"

                    exportLauncher.launch(fileName)
                },
                onDismiss = {
                    showExportPasswordDialog = false
                    pendingExportPassword?.fill('\u0000')
                    pendingExportPassword = null
                }
            )
        }

        if (showImportPasswordDialog) {
            PasswordDialog(
                title = stringResource(R.string.import_data),
                description = stringResource(R.string.enter_the_password_you_used_when_creating_this_backup),
                confirmText = stringResource(R.string.choose_file),
                requireConfirmation = false,
                onConfirm = { password ->
                    showImportPasswordDialog = false
                    pendingImportPassword = password
                    importLauncher.launch(arrayOf("*/*"))
                },
                onDismiss = {
                    showImportPasswordDialog = false
                    pendingImportPassword?.fill('\u0000')
                    pendingImportPassword = null
                }
            )
        }

    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun AboutItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp, horizontal = 16.dp)
        .clickable {
            onClick()
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null,
            modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onTertiary)
        Text(text)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
fun SettingsItem(title: String, description: String, control: @Composable () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp, horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray, modifier = Modifier.width(230.dp))
            }
            control()
        }
    }
}

@Composable
fun SettingsSystemItem(title: String, description: String, icon: ImageVector, iconColor: Color,
                       onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp, horizontal = 16.dp)) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column() {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            Icon(icon, contentDescription = null, tint = iconColor,
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun <T> SettingsDropdown(selected: T, options: List<T>, label: @Composable (T) -> String, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(
                0.8.dp,
                MaterialTheme.colorScheme.outline
            ),
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(label(selected))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PasswordDialog(
    title: String,
    description: String,
    confirmText: String,
    requireConfirmation: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val passwordsMatch = !requireConfirmation || password == confirmPassword
    val canConfirm = password.length >= 6 && passwordsMatch

    AlertDialog(
        onDismissRequest = {
            password = ""
            confirmPassword = ""
            onDismiss()
        },
        title = { Text(title) },
        text = {
            Column {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
                        cursorColor = MaterialTheme.colorScheme.tertiary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedPlaceholderColor = Color.Gray,

                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                if (requireConfirmation) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
                            cursorColor = MaterialTheme.colorScheme.tertiary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedPlaceholderColor = Color.Gray

                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.passwords_don_t_match),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (password.isNotEmpty() && password.length < 6) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.password_must_be_at_least_6_characters),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val passwordChars = password.toCharArray()
                    password = ""
                    confirmPassword = ""
                    onConfirm(passwordChars) },
                enabled = canConfirm
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        textContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}