package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.domain.model.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RateLimiter(
    private val settings: () -> AppSettings,
) {
    constructor(snapshot: AppSettings) : this({ snapshot })
    private val _cooldownUntil = MutableStateFlow(0L)
    val cooldownUntil: StateFlow<Long> = _cooldownUntil.asStateFlow()

    fun remainingDaily(usedToday: Int): Int =
        (settings().dailyLimit - usedToday).coerceAtLeast(0)

    fun underDailyLimit(usedToday: Int): Boolean = usedToday < settings().dailyLimit

    fun triggerCooldown(now: Long = System.currentTimeMillis()) {
        _cooldownUntil.value = now + settings().cooldownMinutes * 60_000L
    }

    fun inCooldown(now: Long = System.currentTimeMillis()): Boolean = now < _cooldownUntil.value

    suspend fun awaitSlot() {
        val interval = settings().operationIntervalMs.coerceAtLeast(0L)
        if (interval > 0) delay(interval)
        while (inCooldown()) {
            delay(200L)
        }
    }
}
