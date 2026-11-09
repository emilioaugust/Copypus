package com.emilioaugust.copypus

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.emilioaugust.copypus.data.database.AppDatabase
import com.emilioaugust.copypus.data.datastore.SettingsDataStore
import com.emilioaugust.copypus.data.entity.ClipboardItem
import com.emilioaugust.copypus.data.repository.ClipboardRepository
import com.emilioaugust.copypus.utils.ImageClipboardSaver
import com.emilioaugust.copypus.utils.LocaleHelper
import com.emilioaugust.copypus.widget.ClipboardWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SaveClipboardActivity : ComponentActivity() {

    private lateinit var repository: ClipboardRepository

    companion object {
        const val EXTRA_FROM_ACCESSIBILITY =
            "from_accessibility"

        private const val TAG =
            "SaveClipboardActivity"

        private const val CAPTURE_DELAY_MS = 150L
    }

    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking {
            SettingsDataStore(newBase)
                .language
                .first()
        }

        super.attachBaseContext(
            LocaleHelper.setLocale(
                newBase,
                language.code
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fromAccessibility =
            intent.getBooleanExtra(
                EXTRA_FROM_ACCESSIBILITY,
                false
            )

        repository = ClipboardRepository(
            this,
            AppDatabase
                .getInstance(applicationContext)
                .clipboardDao()
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (!hasFocus) return
        if (isFinishing) return

        lifecycleScope.launch {
            delay(CAPTURE_DELAY_MS)
            saveClipboard()
            finish()
        }
    }

    private suspend fun saveClipboard() {
        val monitoringEnabled = SettingsDataStore(applicationContext)
            .monitoringEnabledFlow
            .first()

        if (!monitoringEnabled) {
            Log.d(TAG, "Monitoring is paused. Skipping clipboard save.")
            return
        }

        val clipboardManager =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        try {
            val clip = clipboardManager.primaryClip
                ?: return

            if (clip.itemCount <= 0) {
                return
            }

            val item = clip.getItemAt(0)

            val isImage =
                clip.description.hasMimeType("image/*") ||
                        item.uri?.let { uri ->
                            contentResolver
                                .getType(uri)
                                ?.startsWith("image/") == true
                        } == true

            if (isImage && item.uri != null) {
                saveClipboardImage(item.uri!!)
                return
            }

            val text = item
                .coerceToText(this@SaveClipboardActivity)
                ?.toString()
                ?.takeIf { it.isNotBlank() }

            if (text == null) {
                return
            }

            repository.insertItem(
                ClipboardItem(
                    text = text,
                    type = "TEXT",
                    timestamp = System.currentTimeMillis()
                )
            )

            updateClipboardWidget()

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@SaveClipboardActivity,
                    getString(R.string.clipboard_saved_text),
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Clipboard access denied", e)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to read clipboard", e)
        }
    }

    private suspend fun saveClipboardImage(uri: Uri) {
        try {
            val savedImage = ImageClipboardSaver.save(
                context = this@SaveClipboardActivity,
                uri = uri
            )

            val imageFile = savedImage.file
            val imageHash = savedImage.hash

            if (repository.getImageByHash(imageHash) != null) {
                imageFile.delete()

                return
            }

            repository.saveImage(
                imageFileName = imageFile.name,
                imagePath = imageFile.absolutePath,
                imageHash = imageHash
            )


            updateClipboardWidget()

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@SaveClipboardActivity,
                    getString(R.string.clipboard_saved_text),
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to save clipboard image",
                e
            )
        }
    }

    private suspend fun updateClipboardWidget() {
        ClipboardWidget().updateAll(applicationContext)
    }
}


