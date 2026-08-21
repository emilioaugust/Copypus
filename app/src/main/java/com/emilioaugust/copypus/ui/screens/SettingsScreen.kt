package com.emilioaugust.copypus.ui.screens

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilioaugust.copypus.BuildConfig
import com.emilioaugust.copypus.R
import com.emilioaugust.copypus.data.AutoDeleteOption
import com.emilioaugust.copypus.data.AppLanguage
import com.emilioaugust.copypus.data.PauseDuration
import com.emilioaugust.copypus.data.SettingsViewModel
import com.emilioaugust.copypus.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val autoDelete by viewModel.autoDelete.collectAsState(initial = AutoDeleteOption.NEVER)
    val language by viewModel.language.collectAsState(initial = AppLanguage.ENGLISH)
    val pauseDuration by viewModel.pauseDuration.collectAsState(initial = PauseDuration.MIN_15)

    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.topbar_settings)) },
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

                // AUTO DELETE and PAUSE DURATION
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = stringResource(R.string.title_preferences_settings), style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                SettingsCard {
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

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply { data = "mailto:emiliooaaugust@gmail.com".toUri() }
                            context.startActivity(intent)

                        }
                    ) {
                        Icon(Icons.Default.Feedback, contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onTertiary)
                        Text(stringResource(R.string.app_feedback_support_title))
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }

                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(start = 16.dp, end = 16.dp))

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/emilioaugust/Copypus".toUri()))
                        }
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onTertiary)
                        Text(stringResource(R.string.view_source_code_title))
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
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