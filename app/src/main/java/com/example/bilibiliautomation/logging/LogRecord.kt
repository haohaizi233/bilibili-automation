package com.example.bilibiliautomation.logging

data class LogRecord(
    val timestamp: Long,
    val timestampIso: String,
    val level: LogLevel,
    val message: String,
    val taskId: Long? = null,
    val accountId: Long? = null,
    val workflowState: String? = null,
    val action: String? = null,
    val url: String? = null,
    val selector: String? = null,
    val durationMs: Long? = null,
    val resultCode: String? = null,
) {
    fun formatLine(): String {
        val parts = buildList {
            add(timestampIso)
            add(level.name)
            if (taskId != null) add("task=$taskId")
            if (accountId != null) add("account=$accountId")
            if (!workflowState.isNullOrBlank()) add("state=$workflowState")
            if (!action.isNullOrBlank()) add("action=$action")
            if (!selector.isNullOrBlank()) add("selector=$selector")
            if (durationMs != null) add("duration=$durationMs")
            if (!resultCode.isNullOrBlank()) add("result=$resultCode")
            if (!url.isNullOrBlank()) add("url=$url")
            if (message.isNotBlank()) add(message)
        }
        return parts.joinToString(" ")
    }
}
