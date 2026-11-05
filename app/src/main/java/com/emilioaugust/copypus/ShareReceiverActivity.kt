package com.emilioaugust.copypus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.emilioaugust.copypus.data.database.AppDatabase
import com.emilioaugust.copypus.data.datastore.SettingsDataStore
import com.emilioaugust.copypus.data.entity.ClipboardItem
import com.emilioaugust.copypus.data.repository.ClipboardRepository
import com.emilioaugust.copypus.utils.ImageClipboardSaver
import com.emilioaugust.copypus.utils.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ShareReceiverActivity : ComponentActivity() {
    private lateinit var repository: ClipboardRepository

    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking {
            SettingsDataStore(newBase).language.first()
        }
        super.attachBaseContext(
            LocaleHelper.setLocale(newBase, language.code)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = ClipboardRepository(
            this,
            AppDatabase
                .getInstance(this)
                .clipboardDao()
        )

        handleShareIntent(intent)

        setContent {
            ShareReceiverScreen()
        }
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) {
            finish()
            return
        }

        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        if (uri != null) {
            lifecycleScope.launch {
                try {
                    val savedImage = ImageClipboardSaver.save(
                        context = this@ShareReceiverActivity,
                        uri = uri
                    )

                    val imageFile = savedImage.file
                    val imageHash = savedImage.hash

                    val existingImage = repository.getImageByHash(imageHash)

                    if (existingImage == null) {
                        repository.saveImage(
                            imageFileName = imageFile.name,
                            imagePath = imageFile.absolutePath,
                            imageHash = imageHash
                        )
                    } else {
                        imageFile.delete()
                    }

                    Toast.makeText(
                        this@ShareReceiverActivity,
                        getString(R.string.saved_to_copypus),
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    Log.e(
                        "ShareReceiverActivity",
                        "Failed to save shared image",
                        e
                    )
                }
                finish()
            }
            return
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)

        if (!text.isNullOrBlank()) {
            lifecycleScope.launch {
                repository.insertItem(
                    ClipboardItem(
                        text = text,
                        type = "TEXT",
                        timestamp = System.currentTimeMillis()
                    )
                )

                Toast.makeText(
                    this@ShareReceiverActivity,
                    getString(R.string.saved_to_copypus),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            return
        }
        finish()
    }
}

@Composable
fun ShareReceiverScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 18.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.saved_to_copypus),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}