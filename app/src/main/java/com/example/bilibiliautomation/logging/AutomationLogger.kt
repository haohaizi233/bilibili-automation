package com.example.bilibiliautomation.logging

import android.util.Log
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Structured automation logger.
 *
 * Keep this as a process-wide sink so WebView console, workflow, and UI share one stream.
 * Callers should pass task/account/state when known; do not log secrets (cookies).
 */
object AutomationLogger {
    const val TAG: String = "WebViewAutomation"

    fun interface Listener {
        fun onLog(line: String)
    }

    private val listeners = CopyOnWriteArrayList<Listener>()
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    private val _records = MutableStateFlow<List<LogRecord>>(emptyList())
    val records: StateFlow<List<LogRecord>> = _records.asStateFlow()

    private val _events = MutableSharedFlow<LogRecord>(extraBufferCapacity = 256)
    val events: SharedFlow<LogRecord> = _events.asSharedFlow()

    @Volatile
    var lastAction: String = "-"
        private set

    @Volatile
    var lastError: String = "-"
        private set

    @Volatile
    var currentTaskId: Long? = null

    @Volatile
    var currentAccountId: Long? = null

    @Volatile
    var currentWorkflowState: String? = null

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun d(message: String) = log(LogLevel.DEBUG, message)

    fun i(message: String) = log(LogLevel.INFO, message)

    fun success(message: String) = log(LogLevel.SUCCESS, message)

    fun w(message: String) = log(LogLevel.WARN, message)

    fun e(message: String, error: Throwable? = null) {
        lastError = message
        log(LogLevel.ERROR, message, error = error)
    }

    fun web(level: String, message: String) {
        val mapped = when (level.uppercase(Locale.US)) {
            "ERROR" -> LogLevel.ERROR
            "WARN", "WARNING" -> LogLevel.WARN
            else -> LogLevel.INFO
        }
        log(mapped, "[WEB][$level] $message")
    }

    fun action(
        action: String,
        selector: String? = null,
        url: String? = null,
        durationMs: Long,
        result: String,
        error: String? = null,
        resultCode: String? = null,
        taskId: Long? = currentTaskId,
        accountId: Long? = currentAccountId,
        workflowState: String? = currentWorkflowState,
    ) {
        lastAction = action
        if (!error.isNullOrBlank()) lastError = error
        val code = resultCode ?: result
        val level = when {
            result == "SUCCESS" || result == "PASS" || code == "OK" -> LogLevel.SUCCESS
            result == "TIMEOUT" || code == "TIMEOUT" -> LogLevel.WARN
            else -> LogLevel.ERROR
        }
        val message = error?.takeIf { it.isNotBlank() }.orEmpty()
        emit(
            LogRecord(
                timestamp = TimeMs.wallClock(),
                timestampIso = isoFormat.format(Date()),
                level = level,
                message = message,
                taskId = taskId,
                accountId = accountId,
                workflowState = workflowState,
                action = action,
                url = url,
                selector = selector,
                durationMs = durationMs,
                resultCode = code,
            ),
        )
    }

    fun log(
        level: LogLevel,
        message: String,
        action: String? = null,
        selector: String? = null,
        url: String? = null,
        durationMs: Long? = null,
        resultCode: String? = null,
        error: Throwable? = null,
        taskId: Long? = currentTaskId,
        accountId: Long? = currentAccountId,
        workflowState: String? = currentWorkflowState,
    ) {
        if (level == LogLevel.ERROR) lastError = message
        emit(
            LogRecord(
                timestamp = TimeMs.wallClock(),
                timestampIso = isoFormat.format(Date()),
                level = level,
                message = message,
                taskId = taskId,
                accountId = accountId,
                workflowState = workflowState,
                action = action,
                url = url,
                selector = selector,
                durationMs = durationMs,
                resultCode = resultCode,
            ),
            error,
        )
    }

    fun timed(): Long = TimeMs.now()

    fun elapsed(startedAt: Long): Long = TimeMs.now() - startedAt

    fun clear() {
        _records.value = emptyList()
    }

    private fun emit(record: LogRecord, error: Throwable? = null) {
        val line = record.formatLine()
        val priority = when (record.level) {
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.INFO, LogLevel.SUCCESS -> Log.INFO
            LogLevel.WARN -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
        }
        runCatching {
            Log.println(priority, TAG, line)
            if (error != null) {
                Log.println(priority, TAG, Log.getStackTraceString(error))
            }
        }
        _events.tryEmit(record)
        val next = _records.value + record
        _records.value = if (next.size > 500) next.takeLast(500) else next
        listeners.forEach { listener ->
            runCatching { listener.onLog(line) }
        }
    }
}
