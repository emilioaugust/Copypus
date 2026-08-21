package com.emilioaugust.copypus

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.emilioaugust.copypus.data.AppDatabase
import com.emilioaugust.copypus.data.ClipboardItem
import com.emilioaugust.copypus.data.ClipboardRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {
    private lateinit var repository: ClipboardRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = ClipboardRepository(
            AppDatabase
                .getInstance(this)
                .clipboardDao()
        )
        setContent {
            ShareReceiverScreen(intent = intent, repository = repository, onFinish = { finish() })
        }
    }

    private fun handleShareIntent(intent: Intent?) {
        if(intent?.action != Intent.ACTION_SEND) {
            finish()
            return
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)

        if(text.isNullOrBlank()) {
            finish()
            return
        }

        lifecycleScope.launch {
            repository.insertItem(ClipboardItem(text = text, timestamp = System.currentTimeMillis()))
            Toast.makeText(
                this@ShareReceiverActivity,
                getString(R.string.saved_to_copypus),
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}

@Composable
fun ShareReceiverScreen(intent: Intent, repository: ClipboardRepository, onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                repository.insertItem(
                    ClipboardItem(
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )
                )
                delay(1000)
            }
        }
        onFinish()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White)
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