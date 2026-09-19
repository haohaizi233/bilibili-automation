package com.example.bilibiliautomation.domain.model

data class LoadedVideo(
    val bvid: String,
    val title: String,
    val aid: Long? = null,
    val createdAt: Long? = null,
    val durationSeconds: Int? = null,
    val pic: String = "",
)

data class LoadedComment(
    val commentId: String,
    val userUid: String,
    val isRootComment: Boolean,
    val preview: String,
    val authorName: String = "",
    val createdAt: Long? = null,
    val oid: Long? = null,
)
