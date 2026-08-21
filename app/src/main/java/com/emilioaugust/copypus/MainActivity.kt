package com.emilioaugust.copypus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.emilioaugust.copypus.data.MainViewModel
import com.emilioaugust.copypus.data.SettingsDataStore
import com.emilioaugust.copypus.data.SettingsViewModel
import com.emilioaugust.copypus.data.SettingsViewModelFactory
import com.emilioaugust.copypus.ui.screens.MainScreen
import com.emilioaugust.copypus.ui.theme.ClipboardTheme
import com.emilioaugust.copypus.ui.theme.ThemeMode
import com.emilioaugust.copypus.utils.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(
            SettingsDataStore(applicationContext)
        )
    }

    private lateinit var clipboardHelper: ClipboardManagerHelper
    private lateinit var settingsDataStore: SettingsDataStore

    override fun attachBaseContext(newBase: Context) {
        val context = runBlocking {
            val dataStore = SettingsDataStore(newBase)
            val lang = dataStore.getAppLanguage()
            LocaleHelper.setLocale(newBase, lang.code)
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        enableEdgeToEdge()
        clipboardHelper = ClipboardManagerHelper(this)
        settingsDataStore = SettingsDataStore(applicationContext)
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when(themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM ->
                    isSystemInDarkTheme()
            }
            val language by settingsViewModel.language.collectAsState()

            LaunchedEffect(language) {
                val locales = LocaleListCompat.forLanguageTags(language.code)
                AppCompatDelegate.setApplicationLocales(locales)
            }

            ClipboardTheme(darkTheme = darkTheme) {
                MainScreen(viewModel, settingsViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            settingsViewModel.autoDelete.first().let { option ->
                viewModel.cleanupOldItems(option)
            }
            delay(250)

            val pausedUntil = settingsDataStore.pausedUntilFlow.first()
            if (pausedUntil != 0L && System.currentTimeMillis() > pausedUntil) {
                settingsDataStore.setMonitoringEnabled(true)
            }
            val monitoringEnabled = settingsDataStore.monitoringEnabledFlow.first()
            if (!monitoringEnabled) return@launch

            clipboardHelper.getCurrentClipboardData()
                ?.let { data ->
                    when (data) {
                        is ClipboardData.Text -> {
                            viewModel.saveText(data.text)
                        }
                    }
                }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val sharedText =
                intent.getStringExtra(
                    Intent.EXTRA_TEXT
                )
            if (!sharedText.isNullOrBlank()) {
                viewModel.saveText(sharedText)
            }
        }
    }
}