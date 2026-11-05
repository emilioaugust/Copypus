package com.emilioaugust.copypus.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import com.emilioaugust.copypus.data.database.AppDatabase
import com.emilioaugust.copypus.data.repository.ClipboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CopyLatestClipboardAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val repository = ClipboardRepository(
                context,
                AppDatabase.getInstance(context.applicationContext)
                    .clipboardDao()
            )

            val item = repository.getLatestItem()

            if (item == null) {
                showMessage(context, "No copied items")
                return
            }

            val clipboard = context.getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

            when (item.type) {
                "TEXT" -> {
                    val text = item.text

                    if (text.isNullOrBlank()) {
                        showMessage(context, "No text provided")
                        return
                    }

                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("Copied text", text)
                    )

                    showMessage(context, "Text copied")
                }

                "IMAGE" -> {
                    val path = item.imagePath

                    if (path.isNullOrBlank()) {
                        showMessage(context, "No image path")
                        return
                    }

                    val file = File(path)

                    if (!file.isFile || !file.canRead()) {
                        Log.e("CopyLatestAction", "Image not readable: $path")
                        showMessage(context, "Couldn't find image path")
                        return
                    }

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    val clip = ClipData.newUri(
                        context.contentResolver,
                        "Copied image",
                        uri
                    )

                    clipboard.setPrimaryClip(clip)

                    showMessage(context, "Image copied")
                }

                else -> {
                    Log.e("CopyLatestAction", "Unknown type: ${item.type}")
                    showMessage(context, "Unknown type")
                }
            }
        } catch (e: Exception) {
            Log.e("CopyLatestAction", "Copy failed", e)
            showMessage(context, "Error with copy")
        }
    }

    private suspend fun showMessage(
        context: Context,
        message: String
    ) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}