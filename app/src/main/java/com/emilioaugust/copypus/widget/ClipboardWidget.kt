package com.emilioaugust.copypus.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.unit.ColorProvider
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.emilioaugust.copypus.data.database.AppDatabase
import com.emilioaugust.copypus.data.repository.ClipboardRepository
import com.emilioaugust.copypus.SaveClipboardActivity
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.size
import android.graphics.BitmapFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.ContentScale
import androidx.glance.text.TextStyle
import com.emilioaugust.copypus.MainActivity
import com.emilioaugust.copypus.R
import com.emilioaugust.copypus.data.entity.ClipboardItem
import com.emilioaugust.copypus.ui.theme.lighten
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import androidx.glance.LocalContext

class ClipboardWidget : GlanceAppWidget() {

    @SuppressLint("FlowOperatorInvokedInComposition")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ClipboardRepository(
            context.applicationContext,
            AppDatabase.getInstance(context.applicationContext)
                .clipboardDao()
        )

        val initial = loadLast(context)

        provideContent {
            val latest by repository.observeLatestItem()
                .map { item -> itemToUi(context, item) }
                .flowOn(Dispatchers.IO)
                .collectAsState(initial = initial)

            Content(context, latest)
        }
    }

    private suspend fun loadLast(context: Context): LastItemUi {
        val dao = AppDatabase.getInstance(
            context.applicationContext
        ).clipboardDao()

        return itemToUi(context, dao.getLatestItem())
    }

    private fun itemToUi(context: Context, item: ClipboardItem?): LastItemUi {
        return when {
            item == null -> LastItemUi.Empty

            item.type == "TEXT" -> {
                LastItemUi.Text(item.text)
            }

            item.type == "IMAGE" -> {
                val path = item.imagePath

                if (path.isNullOrBlank()) {
                    LastItemUi.Empty
                } else {
                    val bitmap = loadImageBitmap(path)

                    if (bitmap != null) {
                        LastItemUi.Image(bitmap)
                    } else {
                        Log.e(
                            "ClipboardWidget",
                            "Cannot decode image: $path"
                        )
                        LastItemUi.Empty
                    }
                }
            }

            else -> LastItemUi.Empty
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun Content(context: Context, last: LastItemUi) {
        val widgetContext = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .background(ColorProvider(Color.White))
                .cornerRadius(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (last) {
                is LastItemUi.Text -> Text(
                    text = last.text.orEmpty(),
                    maxLines = 1,
                    style = TextStyle(fontSize = 16.sp),
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                )
                is LastItemUi.Image -> Image(
                    provider = ImageProvider(last.bitmap),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(140.dp)
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentScale = ContentScale.Crop
                )
                LastItemUi.Empty -> Text(
                    text = widgetContext.getString(R.string.nothing_here)
                )
            }

            Spacer(GlanceModifier.height(12.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    text = widgetContext.getString(R.string.save),
                    onClick = actionStartActivity<SaveClipboardActivity>(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ColorProvider(Color(0xFF1E90FF)),
                        contentColor = ColorProvider(Color.White)
                    )
                )
                Spacer(GlanceModifier.width(8.dp))
                Button(
                    text = widgetContext.getString(R.string.get_latest_copied),
                    onClick = actionRunCallback<CopyLatestClipboardAction>(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ColorProvider(Color(0xFF1E90FF).lighten()),
                        contentColor = ColorProvider(Color(0xFF1E90FF))
                    )
                )
            }
        }
    }

    private fun loadImageBitmap(path: String): Bitmap? {
        val original = BitmapFactory.decodeFile(path)
            ?: return null

        val maxSize = 512
        val scale = minOf(
            1f,
            maxSize.toFloat() / maxOf(original.width, original.height)
        )

        if (scale == 1f) return original

        val resized = original.scale(
            (original.width * scale).toInt().coerceAtLeast(1),
            (original.height * scale).toInt().coerceAtLeast(1)
        )

        if (resized !== original) original.recycle()

        return resized
    }

    sealed interface LastItemUi {
        data class Text(val text: String?) : LastItemUi
        data class Image(val bitmap: Bitmap) : LastItemUi
        data object Empty : LastItemUi
    }
}

class ClipboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClipboardWidget()
}