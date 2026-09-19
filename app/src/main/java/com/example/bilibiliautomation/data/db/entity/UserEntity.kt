package com.example.bilibiliautomation.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.bilibiliautomation.domain.model.RelationshipState

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["relationshipState"]),
        Index(value = ["lastSeenAt"]),
    ],
)
data class UserEntity(
    @PrimaryKey val uid: String,
    val nickname: String = "",
    val avatarUrl: String = "",
    val relationshipState: RelationshipState = RelationshipState.UNKNOWN,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val lastActionAt: Long? = null,
)
