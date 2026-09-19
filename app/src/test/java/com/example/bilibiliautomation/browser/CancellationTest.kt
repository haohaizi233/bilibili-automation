package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.ResultCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class CancellationTest {
    @Test
    fun waitCancelsImmediatelyInsteadOfPollingToTimeout() = runBlocking {
        val waiter = Waiter(BrowserConfig(timeoutMs = 30_000, pollIntervalMs = 50))
        val actor = BrowserActor(
            handler = { command ->
                when (command) {
                    is WaitForSelectorCommand -> waiter.retryUntilResult(
                        description = "slow",
                        timeoutMs = command.timeoutMs ?: 30_000,
                    ) {
                        AutomationResult.fail<Boolean>(ResultCode.ELEMENT_NOT_FOUND, "not yet")
                    }
                    is CustomCommand -> command.block()
                    else -> AutomationResult.ok<Unit>()
                }
            },
        )
        val elapsed = measureTimeMillis {
            val job = async {
                actor.submit<Unit>(WaitForSelectorCommand(Locator.css("#never"), 30_000))
            }
            delay(200)
            job.cancel()
            try {
                withTimeout(1_000) { job.await() }
            } catch (_: CancellationException) {
            }
        }
        actor.shutdown()
        assertTrue("cancel should not wait remaining ~29s, elapsed=$elapsed", elapsed < 3_000)
    }
}
