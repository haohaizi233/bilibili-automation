package com.example.bilibiliautomation

import android.app.Application
import android.webkit.WebView
import com.example.bilibiliautomation.app.AppContainer
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.service.AutomationForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutomationApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val startScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        installUncaughtExceptionHandler()
        val enabled = BuildConfig.DEBUG
        WebView.setWebContentsDebuggingEnabled(enabled)
        container = AppContainer(this)
        AutomationLogger.i("WebView debugging enabled = $enabled")
        startScope.launch {
            container.crashRecovery.onAppStart()
            // Rehydrate the task UI (counters / current video / progress) from
            // the latest task + checkpoint so reopening the app keeps the
            // task state instead of showing a blank "当前无任务".
            container.taskScheduler.restoreUiState()
        }
        getSystemService(android.app.NotificationManager::class.java)
            .createNotificationChannel(
                android.app.NotificationChannel(
                    AutomationForegroundService.CHANNEL_ID,
                    "Automation",
                    android.app.NotificationManager.IMPORTANCE_LOW,
                ),
            )
    }

    /**
     * Capture every uncaught exception (incl. coroutine exceptions that escape
     * their scope) into the in-app log so the crash stack is visible on the
     * Log screen instead of only in logcat. The previous handler is still
     * invoked afterwards so the system crash dialog behaves as usual.
     */
    private fun installUncaughtExceptionHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                AutomationLogger.e(
                    "FATAL uncaught on ${thread.name}: ${error.javaClass.name} ${error.message}",
                    error,
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
