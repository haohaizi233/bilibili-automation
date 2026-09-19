package com.example.bilibiliautomation.data.api.wbi

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class WbiKeyManagerTest {
    @Test
    fun ttlCacheAndInvalidate() = runBlocking {
        val clock = AtomicLong(0L)
        val manager = WbiKeyManager(ttlSeconds = 60, monotonic = { clock.get() })
        val keys = WbiKeys("a".repeat(32), "b".repeat(32))
        val fetches = AtomicInteger()
        val first = manager.ensureKeys {
            fetches.incrementAndGet()
            keys
        }
        assertEquals(keys, first)
        clock.set(59_000)
        assertNotNull(manager.currentKeys())
        clock.set(60_000)
        assertNull(manager.currentKeys())
        manager.invalidate()
        assertNull(manager.currentKeys())
        val second = manager.ensureKeys {
            fetches.incrementAndGet()
            keys
        }
        assertEquals(2, fetches.get())
        assertEquals(keys, second)
    }

    @Test
    fun mutexSerializesRefresh() = runBlocking {
        val manager = WbiKeyManager()
        val fetches = AtomicInteger()
        val jobs = (1..16).map {
            async {
                manager.ensureKeys {
                    delay(40)
                    fetches.incrementAndGet()
                    WbiKeys("a".repeat(32), "b".repeat(32))
                }
            }
        }
        jobs.awaitAll()
        assertEquals(1, fetches.get())
    }
}
