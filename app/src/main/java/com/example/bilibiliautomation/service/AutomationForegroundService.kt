package com.example.bilibiliautomation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.bilibiliautomation.AutomationApplication
import com.example.bilibiliautomation.MainActivity
import com.example.bilibiliautomation.R
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.ui.task.TaskUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AutomationForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as AutomationApplication).container
        val scheduler = container.taskScheduler
        when (intent?.action) {
            ACTION_PAUSE -> scheduler.pause()
            ACTION_STOP -> {
                scheduler.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val target = intent.getStringExtra(EXTRA_TARGET_UID).orEmpty()
                val accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1L)
                if (target.isNotBlank() && accountId > 0) {
                    scheduler.start(target, accountId)
                }
            }
            ACTION_RESUME -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId > 0) scheduler.resume(taskId)
            }
        }
        val notification = buildNotification(scheduler.uiState.value)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        if (collectJob == null) {
            collectJob = scope.launch {
                scheduler.uiState.collect { state ->
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(NOTIFICATION_ID, buildNotification(state))
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        collectJob?.cancel()
        scope.cancel()
        AutomationLogger.i("AutomationForegroundService destroyed")
        super.onDestroy()
    }

    private fun buildNotification(state: TaskUiState): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pause = pendingService(ACTION_PAUSE, 1)
        val stop = pendingService(ACTION_STOP, 2)
        val progress = "${state.videoProgress.label} videos, ${state.commentProgress.label} comments"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Automation ${state.status}")
            .setContentText("task=${state.taskId ?: "-"} target=${state.target?.uid ?: "-"} $progress")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "task=${state.taskId ?: "-"}\n" +
                        "target=${state.target?.uid ?: "-"}\n" +
                        "state=${state.workflowState}\n" +
                        "action=${state.currentAction ?: "-"}\n" +
                        progress,
                ),
            )
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pause)
            .addAction(android.R.drawable.ic_delete, "Stop", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun pendingService(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AutomationForegroundService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Automation",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "automation_foreground"
        const val NOTIFICATION_ID = 42
        const val ACTION_START = "com.example.bilibiliautomation.START"
        const val ACTION_PAUSE = "com.example.bilibiliautomation.PAUSE"
        const val ACTION_STOP = "com.example.bilibiliautomation.STOP"
        const val ACTION_RESUME = "com.example.bilibiliautomation.RESUME"
        const val EXTRA_TARGET_UID = "targetUid"
        const val EXTRA_ACCOUNT_ID = "accountId"
        const val EXTRA_TASK_ID = "taskId"

        fun start(context: Context, targetUid: String, accountId: Long) {
            val intent = Intent(context, AutomationForegroundService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TARGET_UID, targetUid)
                .putExtra(EXTRA_ACCOUNT_ID, accountId)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            context.startService(Intent(context, AutomationForegroundService::class.java).setAction(ACTION_PAUSE))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AutomationForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}
