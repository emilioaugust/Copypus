package com.emilioaugust.copypus.data.tiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.emilioaugust.copypus.R
import com.emilioaugust.copypus.data.database.AppDatabase
import com.emilioaugust.copypus.data.repository.ClipboardRepository
import com.emilioaugust.copypus.data.datastore.SettingsDataStore
import com.emilioaugust.copypus.utils.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue


class ClearHistoryTileService : TileService() {
    private val settingsDataStore by lazy { SettingsDataStore(applicationContext) }
    private lateinit var repository: ClipboardRepository

    override fun onCreate() {
        super.onCreate()
        repository = ClipboardRepository(applicationContext, AppDatabase
            .getInstance(applicationContext).clipboardDao()
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartListening() {
        super.onStartListening()
        CoroutineScope(Dispatchers.IO).launch {
            val language = settingsDataStore.language.first()
            val context = LocaleHelper.setLocale(applicationContext, language.code)
            qsTile?.apply {
                label = context.getString(R.string.clear_history_label)
                subtitle = context.getString(R.string.delete_all_copied_subtitle_tile)
                state = Tile.STATE_ACTIVE
                updateTile()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick() {
        super.onClick()
        CoroutineScope(Dispatchers.IO).launch {
            val language = settingsDataStore.language.first()
            val context = LocaleHelper.setLocale(applicationContext, language.code)
            repository.clearAll()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    context.getString(R.string.history_cleared_text),
                    Toast.LENGTH_SHORT
                ).show()

                qsTile?.apply {
                    subtitle = context.getString(R.string.history_cleared_text)
                    updateTile()
                }
            }

            delay(2000)

            withContext(Dispatchers.Main) {
                qsTile?.apply {
                    subtitle = context.getString(R.string.delete_all_copied_subtitle_tile)
                    updateTile()
                }
            }
        }
    }
}