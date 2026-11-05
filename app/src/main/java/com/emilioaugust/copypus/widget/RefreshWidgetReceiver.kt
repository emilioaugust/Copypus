package com.emilioaugust.copypus.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RefreshWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REFRESH) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ClipboardWidget().updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.emilioaugust.widget.ACTION_REFRESH"
    }
}