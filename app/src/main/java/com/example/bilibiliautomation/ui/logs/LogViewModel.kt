package com.example.bilibiliautomation.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.logging.LogLevel
import com.example.bilibiliautomation.logging.LogRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LogViewModel : ViewModel() {
    private val filter = MutableStateFlow(Filter.ALL)

    val records: StateFlow<List<LogRecord>> = combine(AutomationLogger.records, filter) { items, current ->
        items.filter { record ->
            when (current) {
                Filter.ALL -> true
                Filter.INFO -> record.level == LogLevel.INFO || record.level == LogLevel.DEBUG
                Filter.SUCCESS -> record.level == LogLevel.SUCCESS
                Filter.WARN -> record.level == LogLevel.WARN
                Filter.ERROR -> record.level == LogLevel.ERROR
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(value: Filter) {
        filter.value = value
    }

    fun clear() {
        AutomationLogger.clear()
    }

    enum class Filter { ALL, INFO, SUCCESS, WARN, ERROR }
}
