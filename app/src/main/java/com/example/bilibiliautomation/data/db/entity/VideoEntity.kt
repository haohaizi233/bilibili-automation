package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["bvid"], unique = true),
        Index(value = ["targetUid"]),
        Index(value = ["processed"]),
        Index(value = ["createdAt"]),
    ],
)
data class VideoEntity(
    @PrimaryKey val bvid: String,
    val targetUid: String,
    val title: String = "",
    val processed: Boolean = false,
    val lastCommentCursor: String? = null,
    val processedCommentCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val processedAt: Long? = null,
)
