package com.example.bilibiliautomation.domain.model

data class AutomationResult<out T>(
    val success: Boolean,
    val code: String,
    val message: String = "",
    val data: T? = null,
    val durationMs: Long = 0L,
    val metadata: Map<String, String> = emptyMap(),
) {
    val isRateLimited: Boolean get() = code == ResultCode.RATE_LIMITED
    val isAccountRestricted: Boolean get() = code == ResultCode.ACCOUNT_RESTRICTED
    val requiresCooldown: Boolean get() = isRateLimited || isAccountRestricted

    fun <R> map(transform: (T?) -> R?): AutomationResult<R> = AutomationResult(
        success = success,
        code = code,
        message = message,
        data = transform(data),
        durationMs = durationMs,
        metadata = metadata,
    )

    fun withDuration(durationMs: Long): AutomationResult<T> = copy(durationMs = durationMs)

    companion object {
        const val OK: String = ResultCode.OK
        const val ELEMENT_NOT_FOUND: String = ResultCode.ELEMENT_NOT_FOUND
        const val ELEMENT_NOT_VISIBLE: String = ResultCode.ELEMENT_NOT_VISIBLE
        const val ELEMENT_DISABLED: String = ResultCode.ELEMENT_DISABLED
        const val TIMEOUT: String = ResultCode.TIMEOUT
        const val PAGE_LOAD_FAILED: String = ResultCode.NAVIGATION_FAILED
        const val NAVIGATION_FAILED: String = ResultCode.NAVIGATION_FAILED
        const val JS_ERROR: String = ResultCode.JS_ERROR
        const val PAGE_CHANGED: String = ResultCode.PAGE_CHANGED
        const val NETWORK_ERROR: String = ResultCode.NETWORK_ERROR
        const val LOGIN_REQUIRED: String = ResultCode.LOGIN_REQUIRED
        const val LOGIN_EXPIRED: String = ResultCode.LOGIN_EXPIRED
        const val RATE_LIMITED: String = ResultCode.RATE_LIMITED
        const val ACCOUNT_RESTRICTED: String = ResultCode.ACCOUNT_RESTRICTED
        const val CANCELLED: String = ResultCode.CANCELLED
        const val UNKNOWN: String = ResultCode.UNKNOWN
        const val INVALID_ARGUMENT: String = ResultCode.INVALID_ARGUMENT

        fun <T> ok(
            data: T? = null,
            message: String = "",
            code: String = ResultCode.OK,
            durationMs: Long = 0L,
            metadata: Map<String, String> = emptyMap(),
        ): AutomationResult<T> = AutomationResult(
            success = true,
            code = code,
            message = message,
            data = data,
            durationMs = durationMs,
            metadata = metadata,
        )

        fun <T> fail(
            code: String,
            message: String,
            data: T? = null,
            durationMs: Long = 0L,
            metadata: Map<String, String> = emptyMap(),
        ): AutomationResult<T> = AutomationResult(
            success = false,
            code = code,
            message = message,
            data = data,
            durationMs = durationMs,
            metadata = metadata,
        )

        fun <T> cancelled(message: String = "cancelled"): AutomationResult<T> =
            fail(ResultCode.CANCELLED, message)
    }
}
