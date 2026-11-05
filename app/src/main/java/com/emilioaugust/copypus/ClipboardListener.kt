package com.emilioaugust.copypus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.emilioaugust.copypus.data.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class ClipboardData {
    data class Text(val text: String) : ClipboardData()
    data class Image(val uri: Uri) : ClipboardData()
}

class ClipboardManagerHelper(val appContext: Context) {
    private val clipboardManager = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var ignoreNext = false
    private val settingsDataStore = SettingsDataStore(appContext)

    fun getCurrentClipboardData(): ClipboardData? {
        return try {
            val clip = clipboardManager.primaryClip
                ?: return null

            val item = clip.getItemAt(0)

            val isImage =
                clip.description.hasMimeType("image/*") ||
                        item.uri?.let {
                            appContext.contentResolver.getType(it)
                                ?.startsWith("image/") == true
                        } == true

            if (isImage && item.uri != null) {
                return ClipboardData.Image(item.uri!!)
            }

            val text = item
                .coerceToText(appContext)
                ?.toString()

            if (!text.isNullOrBlank()) {
                ClipboardData.Text(text)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun startListening(onNewData: (ClipboardData) -> Unit) {
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            CoroutineScope(Dispatchers.IO).launch {
                val enabled = settingsDataStore.monitoringEnabledFlow.first()
                if (!enabled) return@launch

                val clip = clipboardManager.primaryClip
                val item = clip?.getItemAt(0)

                if (item != null) {
                    val text = item.coerceToText(appContext)?.toString()
                    if (!text.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            onNewData(ClipboardData.Text(text))
                        }
                    }
                }
            }
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
    }

    fun stopListening() {
        listener?.let {
            clipboardManager.removePrimaryClipChangedListener(it)
        }
    }

    fun copyTextToClipboard(text: String?) {
        ignoreNext = true
        val clip = ClipData.newPlainText("Saved text", text)
        clipboardManager.setPrimaryClip(clip)
    }

    fun copyImageToClipboard(imagePath: String) {
        try {
            val imageFile = File(imagePath)

            if (!imageFile.exists()) {
                Toast.makeText(
                    appContext,
                    R.string.image_file_not_found,
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val imageUri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                imageFile
            )

            val clip = ClipData.newUri(
                appContext.contentResolver,
                imageFile.name,
                imageUri
            )

            ignoreNext = true

            clipboardManager.setPrimaryClip(clip)

            Toast.makeText(
                appContext,
                R.string.clipboard_saved_text,
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Log.e(
                "ClipboardManagerHelper",
                "Failed to copy image to clipboard",
                e
            )

            Toast.makeText(
                appContext,
                R.string.couldn_t_copy_image,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
