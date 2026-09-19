package com.example.bilibiliautomation.domain.model

data class Target(
    val id: Long,
    val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastScanAt: Long?,
    val fansCount: Long = -1L,
)
