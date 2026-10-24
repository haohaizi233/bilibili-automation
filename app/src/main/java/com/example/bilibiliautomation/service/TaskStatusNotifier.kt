package com.example.bilibiliautomation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.bilibiliautomation.domain.model.AppSettings
import com.example.bilibiliautomation.event.AutomationEvent

class TaskStatusNotifier(
    private val context: Context,
    private val settings: () -> AppSettings,
) {
    fun onEvent(event: AutomationEvent) {
        if (!settings().notificationEnabled) return
        if (!com.example.bilibiliautomation.util.KeepAlivePermissions.notificationsGranted(context)) return
        val (title, text) = when (event) {
            is AutomationEvent.TaskFinished -> "任务已完成" to "关注任务已结束"
            is AutomationEvent.TaskFailed -> "任务失败" to event.message.ifBlank { "任务执行失败" }
            is AutomationEvent.TaskPaused -> "任务已暂停" to event.reason.ifBlank { "任务已暂停" }
            is AutomationEvent.NeedsReview -> "任务需要处理" to event.message.ifBlank { "请检查账号或任务状态" }
            else -> return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "任务提醒", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "task_alerts"
        private const val NOTIFICATION_ID = 43
    }
}
