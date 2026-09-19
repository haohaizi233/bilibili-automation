package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.domain.model.AutomationResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class BrowserActorSerialTest {
    @Test
    fun twentyConcurrentSubmitsRunSerially() = runBlocking {
        val inFlight = AtomicInteger(0)
        val maxInFlight = AtomicInteger(0)
        val order = CopyOnWriteArrayList<Int>()
        val actor = BrowserActor(
            handler = { command ->
                val current = inFlight.incrementAndGet()
                maxInFlight.updateAndGet { maxOf(it, current) }
                val custom = command as CustomCommand
                val result = custom.block()
                inFlight.decrementAndGet()
                result
            },
        )
        val results = (1..20).map { index ->
            async {
                actor.submit<Int>(
                    CustomCommand("cmd-$index") {
                        delay(8)
                        order.add(index)
                        AutomationResult.ok(index)
                    },
                )
            }
        }.awaitAll()
        actor.shutdown()
        assertEquals(20, results.size)
        assertTrue(results.all { it.success })
        assertEquals(20, order.size)
        assertEquals(1, maxInFlight.get())
        assertEquals(20, actor.processedCount)
    }
}
