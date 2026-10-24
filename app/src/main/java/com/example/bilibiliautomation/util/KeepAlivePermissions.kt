package com.example.bilibiliautomation.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class KeepAliveCheck(
    val notifications: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val ready: Boolean get() = notifications && batteryUnrestricted

    val missingLabels: List<String>
        get() = buildList {
            if (!notifications) add("通知权限（前台常驻）")
            if (!batteryUnrestricted) add("忽略电池优化")
        }
}

enum class KeepAliveNeed {
    READY,
    NOTIFICATION_RUNTIME,
    NOTIFICATION_SETTINGS,
    BATTERY_EXEMPTION,
}

object KeepAlivePermissions {
    fun inspect(context: Context): KeepAliveCheck {
        return KeepAliveCheck(
            notifications = notificationsGranted(context),
            batteryUnrestricted = batteryUnrestricted(context),
        )
    }

    fun notificationsGranted(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun batteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun nextNeed(context: Context): KeepAliveNeed {
        if (!notificationsGranted(context)) {
            return if (needsRuntimeNotificationPermission(context)) {
                KeepAliveNeed.NOTIFICATION_RUNTIME
            } else {
                KeepAliveNeed.NOTIFICATION_SETTINGS
            }
        }
        if (!batteryUnrestricted(context)) return KeepAliveNeed.BATTERY_EXEMPTION
        return KeepAliveNeed.READY
    }

    fun needsRuntimeNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
    }

    fun setKeepScreenOn(activity: Activity, enabled: Boolean) {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun notificationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

    fun batteryExemptionIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
    }

    fun batterySettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    fun appDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
    }
}
