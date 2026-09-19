package com.example.bilibiliautomation.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AutomationEventBus {
    private val _events = MutableSharedFlow<AutomationEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AutomationEvent> = _events.asSharedFlow()

    suspend fun emit(event: AutomationEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: AutomationEvent): Boolean = _events.tryEmit(event)
}
