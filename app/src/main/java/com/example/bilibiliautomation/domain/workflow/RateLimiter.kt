package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.domain.model.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

class RateLimiter(
    private val settings: AppSettings,
) {
    private val _cooldownUntil = MutableStateFlow(0L)
    val cooldownUntil: StateFlow<Long> = _cooldownUntil.asStateFlow()
    private val dailyCount = AtomicInteger(0)

    val remainingDaily: Int
        get() = (settings.dailyLimit - dailyCount.get()).coerceAtLeast(0)

    fun underDailyLimit(): Boolean = dailyCount.get() < settings.dailyLimit

    fun recordAction() {
        dailyCount.incrementAndGet()
    }

    fun triggerCooldown(now: Long = System.currentTimeMillis()) {
        _cooldownUntil.value = now + settings.cooldownMinutes * 60_000L
    }

    fun inCooldown(now: Long = System.currentTimeMillis()): Boolean = now < _cooldownUntil.value

    suspend fun awaitSlot() {
        val interval = settings.operationIntervalMs.coerceAtLeast(0L)
        if (interval > 0) delay(interval)
        while (inCooldown()) {
            delay(200L)
        }
    }
}
