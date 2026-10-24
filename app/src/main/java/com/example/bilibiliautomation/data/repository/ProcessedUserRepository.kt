package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.domain.model.RelationshipState
import java.util.concurrent.ConcurrentHashMap

/**
 * Dedup lives here, not in the HTTP client.
 * Keyed by target UP + comment author so the same task does not reprocess a UID.
 */
class ProcessedUserRepository(
    private val users: UserRepository,
) {
    private val seen = ConcurrentHashMap<String, MutableSet<String>>()

    suspend fun hasProcessed(targetUpUid: String, commentAuthorUid: String): Boolean {
        if (seen[targetUpUid]?.contains(commentAuthorUid) == true) return true
        return users.getByUid(commentAuthorUid) != null
    }

    suspend fun markProcessed(
        targetUpUid: String,
        commentAuthorUid: String,
        nickname: String,
        relationshipState: RelationshipState,
    ) {
        seen.getOrPut(targetUpUid) { ConcurrentHashMap.newKeySet() }.add(commentAuthorUid)
        users.upsertSeen(
            uid = commentAuthorUid,
            nickname = nickname,
            avatarUrl = "",
            relationshipState = relationshipState,
        )
    }

    fun clear() {
        seen.clear()
    }
}
