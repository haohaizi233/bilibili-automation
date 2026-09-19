package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.bilibiliautomation.domain.model.TaskStatus

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["targetUid"]),
        Index(value = ["accountId"]),
        Index(value = ["createdAt"]),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: TaskStatus = TaskStatus.IDLE,
    val targetUid: String? = null,
    val accountId: Long? = null,
    val createdAt: Long,
    val startedAt: Long? = null,
    val pausedAt: Long? = null,
    val finishedAt: Long? = null,
    val lastError: String? = null,
)
