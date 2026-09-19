package com.example.bilibiliautomation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bilibiliautomation.AutomationApplication
import com.example.bilibiliautomation.ui.account.AccountViewModel
import com.example.bilibiliautomation.ui.home.HomeViewModel
import com.example.bilibiliautomation.ui.logs.LogViewModel
import com.example.bilibiliautomation.ui.settings.SettingsViewModel
import com.example.bilibiliautomation.ui.target.TargetViewModel
import com.example.bilibiliautomation.ui.task.TaskViewModel

class AppViewModelFactory(
    private val app: AutomationApplication,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val c = app.container
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(c) as T
            modelClass.isAssignableFrom(AccountViewModel::class.java) -> AccountViewModel(c) as T
            modelClass.isAssignableFrom(TargetViewModel::class.java) -> TargetViewModel(c) as T
            modelClass.isAssignableFrom(TaskViewModel::class.java) -> TaskViewModel(c) as T
            modelClass.isAssignableFrom(LogViewModel::class.java) -> LogViewModel() as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(c) as T
            else -> throw IllegalArgumentException("unknown ViewModel ${modelClass.name}")
        }
    }
}
