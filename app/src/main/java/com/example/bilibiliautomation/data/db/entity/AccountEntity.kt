package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.bilibiliautomation.domain.model.AccountStatus

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["sessionProfileId"]),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val nickname: String = "",
    val avatarUrl: String = "",
    val status: AccountStatus = AccountStatus.LOGIN_REQUIRED,
    val createdAt: Long,
    val updatedAt: Long,
    val lastLoginAt: Long? = null,
    val lastValidatedAt: Long? = null,
    val cooldownUntil: Long? = null,
    val disabled: Boolean = false,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val sessionProfileId: String? = null,
    val fansCount: Long = -1L,
    val followingCount: Long = -1L,
)
