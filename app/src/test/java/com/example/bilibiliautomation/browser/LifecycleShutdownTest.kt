package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.domain.model.AutomationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecycleShutdownTest {
    @Test
    fun shutdownStopsFurtherWork() = runBlocking {
        val actor = BrowserActor(
            handler = { command -> (command as CustomCommand).block() },
        )
        actor.submit<Unit>(CustomCommand("ok") { AutomationResult.ok<Unit>() })
        actor.shutdown()
        val after = actor.submit<Unit>(CustomCommand("late") { AutomationResult.ok<Unit>() })
        assertTrue(!after.success)
        assertEquals("CANCELLED", after.code)
    }
}
