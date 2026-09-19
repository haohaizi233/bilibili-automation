package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "action_history",
    indices = [
        Index(value = ["accountUid"]),
        Index(value = ["targetUid"]),
        Index(value = ["sourceVideoId"]),
        Index(value = ["createdAt"]),
    ],
)
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountUid: String,
    val targetUid: String?,
    val sourceTargetUid: String?,
    val sourceVideoId: String?,
    val sourceCommentId: String?,
    val actionType: String,
    val beforeState: String?,
    val afterState: String?,
    val resultCode: String,
    val resultMessage: String?,
    val createdAt: Long,
)
