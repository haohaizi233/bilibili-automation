package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
    indices = [
        Index(value = ["commentId"], unique = true),
        Index(value = ["videoId"]),
        Index(value = ["userUid"]),
        Index(value = ["isRootComment"]),
        Index(value = ["createdAt"]),
    ],
)
data class CommentEntity(
    @PrimaryKey val commentId: String,
    val videoId: String,
    val userUid: String,
    val isRootComment: Boolean = true,
    val processed: Boolean = false,
    val createdAt: Long,
    val processedAt: Long? = null,
)
