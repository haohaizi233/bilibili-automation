package com.example.bilibiliautomation.domain.model

enum class TaskStatus {
    IDLE,
    QUEUED,
    RUNNING,
    PAUSED,
    COOLDOWN,
    FINISHED,
    FAILED,
    CANCELLED,
    INTERRUPTED,
}
