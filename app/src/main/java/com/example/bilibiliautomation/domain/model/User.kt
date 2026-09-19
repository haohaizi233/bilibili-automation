package com.example.bilibiliautomation.domain.model

data class User(
    val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val relationshipState: RelationshipState,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val lastActionAt: Long?,
)
