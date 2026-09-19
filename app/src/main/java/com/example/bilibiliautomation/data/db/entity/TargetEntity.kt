package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "targets",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["enabled"]),
        Index(value = ["createdAt"]),
    ],
)
data class TargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val nickname: String = "",
    val avatarUrl: String = "",
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val lastScanAt: Long? = null,
    val fansCount: Long = -1L,
)
