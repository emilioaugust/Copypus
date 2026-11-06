package com.emilioaugust.copypus.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.emilioaugust.copypus.R
import com.emilioaugust.copypus.service.ClipboardAccessibilityService
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


// FORMAT TIME

fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun formatSectionTitle(timestamp: Long, context: Context): String {
    val now = Calendar.getInstance()
    val itemDate = Calendar.getInstance()

    itemDate.timeInMillis = timestamp
    return when {
        isSameDay(now, itemDate) -> { context.getString(R.string.today_section) }
        isYesterday(now, itemDate) -> { context.getString(R.string.yesterday_section) }
        else -> {
            DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                context.resources.configuration.locales[0]
            ).format(Date(timestamp))
        }
    }
}

fun isSameDay(first: Calendar, second: Calendar): Boolean {
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
            && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

fun isYesterday(today: Calendar, itemDate: Calendar): Boolean {
    val yesterday = Calendar.getInstance()
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(yesterday, itemDate)
}

// TYPE

enum class ClipboardType {
    LINK,
    CODE,
    TEXT,
    IMAGE
}

fun detectClipboardType(text: String): ClipboardType {
    val lower = text.lowercase()
    val isLink =
        lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.startsWith("www.")

    if (isLink) {
        return ClipboardType.LINK
    }

    val codeKeywords = listOf(
        "fun ", "class ", "val ", "var ", "const ", "import ",
        "public ", "private ", "return ", "if(", "if (", "else",
        "{", "}", ";", "<?php", "console.log", "println", "System.out",
        "#include", "def ", "print(", "SELECT ", "INSERT ", "UPDATE "
    )

    val isCode = codeKeywords.any { lower.contains(it.lowercase()) }

    if (isCode) {
        return ClipboardType.CODE
    }

    return ClipboardType.TEXT
}

// SYSTEM
fun isPostNotificationsGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun checkNotificationEnabled(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }
}

fun isBatteryOptimizationIgnored(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return true
    }

    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
fun requestIgnoreBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return
    }

    val packageUri = "package:${context.packageName}".toUri()

    try {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            packageUri
        )

        context.startActivity(intent)

    } catch (e: ActivityNotFoundException) {

        try {
            val intent = Intent(
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            )

            context.startActivity(intent)

        } catch (e: ActivityNotFoundException) {
        }
    } catch (e: SecurityException) {

    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponent = ComponentName(
        context,
        ClipboardAccessibilityService::class.java
    )

    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)

    while (splitter.hasNext()) {
        val component = ComponentName.unflattenFromString(splitter.next())
        if (component == expectedComponent) {
            return true
        }
    }
    return false
}

fun isOverlayPermissionGranted(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}