package com.example.bilibiliautomation.domain.model

data class Video(
    val bvid: String,
    val targetUid: String,
    val title: String,
    val processed: Boolean,
    val lastCommentCursor: String?,
    val processedCommentCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val processedAt: Long?,
)
