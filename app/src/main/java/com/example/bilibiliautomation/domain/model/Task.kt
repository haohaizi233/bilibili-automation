package com.example.bilibiliautomation.domain.model

data class Task(
    val id: Long,
    val status: TaskStatus,
    val targetUid: String?,
    val accountId: Long?,
    val groupId: Long? = null,
    val groupIndex: Int = 0,
    val createdAt: Long,
    val startedAt: Long?,
    val pausedAt: Long?,
    val finishedAt: Long?,
    val lastError: String?,
)
