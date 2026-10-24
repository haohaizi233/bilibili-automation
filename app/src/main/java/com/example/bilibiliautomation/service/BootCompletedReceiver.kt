package com.example.bilibiliautomation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.bilibiliautomation.AutomationApplication
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.KeepAlivePermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_HANDLED_BOOT, Int.MIN_VALUE) == bootCount) {
            AutomationLogger.w("boot: ignoring duplicate BOOT_COMPLETED for bootCount=$bootCount")
            return
        }
        prefs.edit().putInt(KEY_HANDLED_BOOT, bootCount).apply()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as? AutomationApplication ?: return@launch
                val settings = app.container.settingsRepository.settings.first()
                if (!settings.unattendedMode) return@launch
                if (!KeepAlivePermissions.inspect(context).ready) {
                    AutomationLogger.w("boot: unattended mode on but keep-alive permissions missing — auto off")
                    app.container.settingsRepository.update { it.copy(unattendedMode = false) }
                    return@launch
                }
                AutomationLogger.i("boot: attaching unattended foreground service")
                AutomationForegroundService.attach(context)
            } catch (error: Exception) {
                AutomationLogger.e("boot attach failed", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val PREFS = "keep_alive_boot"
        private const val KEY_HANDLED_BOOT = "handled_boot_count"
    }
}
