package com.example.bilibiliautomation.domain.api

import com.example.bilibiliautomation.domain.model.RelationshipState

sealed interface RelationState {
    data object None : RelationState
    data object Following : RelationState
    data object Follower : RelationState
    data object Mutual : RelationState
    data object Unknown : RelationState

    val isFollowing: Boolean
        get() = this is Following || this is Mutual

    fun toRelationshipState(): RelationshipState = when (this) {
        None -> RelationshipState.NOT_FOLLOWING
        Following, Follower -> RelationshipState.FOLLOWING
        Mutual -> RelationshipState.MUTUAL
        Unknown -> RelationshipState.UNKNOWN
    }

    companion object {
        /**
         * Maps `relation.attribute` from Bilibili relation payloads.
         * Values come from documented web API attribute codes, not from 参考 spider.py.
         * 0 none, 1 quiet-follow, 2 following, 6 mutual, 128 blocked.
         */
        fun fromAttribute(attribute: Int?): RelationState = when (attribute) {
            0 -> None
            1, 2 -> Following
            6 -> Mutual
            else -> Unknown
        }
    }
}
