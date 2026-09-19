package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.logging.AutomationLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class Waiter(
    private val config: BrowserConfig,
    private val logger: AutomationLogger = AutomationLogger,
) {
    suspend fun waitUntil(
        description: String,
        timeoutMs: Long = config.timeoutMs,
        pollIntervalMs: Long = config.pollIntervalMs,
        condition: suspend () -> Boolean,
    ): AutomationResult<Unit> {
        val started = logger.timed()
        val completed = withTimeoutOrNull(timeoutMs) {
            while (true) {
                if (condition()) return@withTimeoutOrNull true
                delay(pollIntervalMs)
            }
        }
        val duration = logger.elapsed(started)
        return if (completed == true) {
            logger.action("WAIT", selector = description, durationMs = duration, result = "SUCCESS", resultCode = ResultCode.OK)
            AutomationResult.ok(message = description, durationMs = duration)
        } else {
            logger.action(
                action = "WAIT",
                selector = description,
                durationMs = duration,
                result = "TIMEOUT",
                error = "timeout after ${timeoutMs}ms waiting for $description",
                resultCode = ResultCode.TIMEOUT,
            )
            AutomationResult.fail(
                ResultCode.TIMEOUT,
                "timeout after ${timeoutMs}ms waiting for $description",
                durationMs = duration,
            )
        }
    }

    suspend fun <T> retryUntilResult(
        description: String,
        timeoutMs: Long = config.timeoutMs,
        pollIntervalMs: Long = config.pollIntervalMs,
        block: suspend () -> AutomationResult<T>,
    ): AutomationResult<T> {
        val started = logger.timed()
        try {
            val winner = withTimeoutOrNull(timeoutMs) {
                while (true) {
                    val current = block()
                    if (current.success) return@withTimeoutOrNull current
                    delay(pollIntervalMs)
                }
                @Suppress("UNREACHABLE_CODE")
                null
            }
            if (winner != null) return winner.withDuration(logger.elapsed(started))
            val duration = logger.elapsed(started)
            logger.action(
                action = "WAIT",
                selector = description,
                durationMs = duration,
                result = "TIMEOUT",
                error = "timeout after ${timeoutMs}ms waiting for $description",
                resultCode = ResultCode.TIMEOUT,
            )
            return AutomationResult.fail(
                ResultCode.TIMEOUT,
                "timeout after ${timeoutMs}ms waiting for $description",
                durationMs = duration,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }
}
