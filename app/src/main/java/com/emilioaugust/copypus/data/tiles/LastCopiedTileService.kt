package com.emilioaugust.copypus.data.tiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LastCopiedTileService : TileService() {
    private val settingsDataStore by lazy { SettingsDataStore(applicationContext) }
    private lateinit var repository: ClipboardRepository

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(applicationContext)
        repository = ClipboardRepository(applicationContext, database.clipboardDao())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartListening() {
        super.onStartListening()
        CoroutineScope(Dispatchers.IO).launch {
            val item = repository.getLatestItem()
            withContext(Dispatchers.Main) {
                updateTile(item?.text)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick() {
        super.onClick()
        CoroutineScope(Dispatchers.IO).launch {
            val language = settingsDataStore.language.first()
            val context = LocaleHelper.setLocale(applicationContext, language.code)
            val item = repository.getLatestItem()
            if (item != null) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.copied_text), item.text)
                clipboard.setPrimaryClip(clip)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        context.getString(R.string.copied_text),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateTile(text: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            val language = settingsDataStore.language.first()
            val context = LocaleHelper.setLocale(applicationContext, language.code)
            qsTile?.apply {
                label = context.getString(R.string.last_copied_tile_name)
                subtitle =
                    if (text.isNullOrBlank()) {
                        context.getString(R.string.no_items_qs_tile_subtitle)
                    } else {
                        text.take(20)
                    }
                state = Tile.STATE_ACTIVE
                updateTile()
            }
        }
    }
}