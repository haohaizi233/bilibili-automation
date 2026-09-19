package com.example.bilibiliautomation.domain.model

data class Comment(
    val commentId: String,
    val videoId: String,
    val userUid: String,
    val isRootComment: Boolean,
    val processed: Boolean,
    val createdAt: Long,
    val processedAt: Long?,
)
