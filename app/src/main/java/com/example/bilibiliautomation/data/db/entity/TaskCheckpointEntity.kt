package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_checkpoints",
    indices = [
        Index(value = ["taskId"], unique = true),
        Index(value = ["updatedAt"]),
    ],
)
data class TaskCheckpointEntity(
    @PrimaryKey val taskId: Long,
    val workflowState: String,
    val targetUid: String? = null,
    val videoId: String? = null,
    val videoIndex: Int = 0,
    val commentId: String? = null,
    val commentCursor: String? = null,
    val commentIndex: Int = 0,
    val currentUserUid: String? = null,
    val currentUrl: String? = null,
    val followed: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val videoTotal: Int = 0,
    val commentTotal: Int = 0,
    val currentVideoTitle: String? = null,
    val currentVideoCover: String? = null,
    val updatedAt: Long,
)
