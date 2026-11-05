package com.emilioaugust.copypus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.content.FileProvider
import com.emilioaugust.copypus.data.database.AppDatabase
import com.emilioaugust.copypus.data.datastore.SettingsDataStore
import com.emilioaugust.copypus.data.repository.ClipboardRepository
import com.emilioaugust.copypus.utils.LocaleHelper
import kotlinx.coroutines.flow.first

class ReadLastClipboardActivity : ComponentActivity() {

    private val settingsDataStore by lazy { SettingsDataStore(applicationContext) }
    private lateinit var repository: ClipboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Защита: запускать только через ярлык / явный intent
        val action = intent?.action
        if (action != ACTION_READ_LAST && action != android.content.Intent.ACTION_VIEW) {
            finish()
            return
        }

        val database = AppDatabase.getInstance(applicationContext)
        repository = ClipboardRepository(applicationContext, database.clipboardDao())

        lifecycleScope.launch {
            // локализация как в TileService
            val language = settingsDataStore.language.first()
            val localizedContext = LocaleHelper.setLocale(applicationContext, language.code)

            val item = repository.getLatestItem()

            if (item == null) {
                showToast(localizedContext, localizedContext.getString(R.string.no_items_qs_tile_subtitle))
                finish()
                return@launch
            }

            when (item.type) {
                "TEXT" -> {
                    val text = item.text
                    if (text.isNullOrBlank()) {
                        showToast(localizedContext, "Nothing here")
                        finish()
                        return@launch
                    }
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            localizedContext.getString(R.string.copied_text),
                            text
                        )
                    )
                    showToast(localizedContext, localizedContext.getString(R.string.copied_text))
                }

                "IMAGE" -> {
                    val path = item.imagePath
                    if (path.isNullOrBlank()) {
                        showToast(localizedContext, "No image path")
                        finish()
                        return@launch
                    }
                    val file = File(path)
                    if (!file.isFile || !file.canRead()) {
                        Log.e(TAG, "Image not readable: $path")
                        showToast(localizedContext, "Couldn't find image")
                        finish()
                        return@launch
                    }
                    try {
                        val uri = FileProvider.getUriForFile(
                            this@ReadLastClipboardActivity,
                            "${packageName}.fileprovider",
                            file
                        )
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newUri(
                                contentResolver,
                                "Copied image",
                                uri
                            )
                        )
                        showToast(localizedContext, "Copied image")
                    } catch (e: Exception) {
                        Log.e(TAG, "Image copy failed", e)
                        showToast(localizedContext, "Error with copy")
                    }
                }

                else -> {
                    Log.e(TAG, "Unknown type: ${item.type}")
                    showToast(localizedContext, "Unknown type")
                }
            }

            finish()
        }
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "ReadLastClipboardActivity"
        const val ACTION_READ_LAST = "com.emilioaugust.copypus.action.READ_LAST"
    }
}