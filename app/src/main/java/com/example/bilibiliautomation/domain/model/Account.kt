package com.example.bilibiliautomation.domain.model

data class Account(
    val id: Long,
    val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val status: AccountStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val lastLoginAt: Long?,
    val lastValidatedAt: Long?,
    val cooldownUntil: Long?,
    val disabled: Boolean,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
    val sessionProfileId: String?,
    val fansCount: Long = -1L,
    val followingCount: Long = -1L,
)
