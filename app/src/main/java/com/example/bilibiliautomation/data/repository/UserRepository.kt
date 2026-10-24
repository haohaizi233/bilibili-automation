package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.UserDao
import com.example.bilibiliautomation.data.db.entity.UserEntity
import com.example.bilibiliautomation.domain.model.RelationshipState
import com.example.bilibiliautomation.domain.model.User
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    private val dao: UserDao,
) {
    suspend fun getByUid(uid: String): User? = withContext(Dispatchers.IO) {
        dao.getByUid(uid)?.toModel()
    }

    /**
     * Dedup by primary key. A second insert of the same UID is ignored.
     * UNKNOWN is stored as UNKNOWN — never coerced to NOT_FOLLOWING.
     */
    suspend fun upsertSeen(
        uid: String,
        nickname: String,
        avatarUrl: String,
        relationshipState: RelationshipState,
    ): User = withContext(Dispatchers.IO) {
        val now = TimeMs.wallClock()
        val existing = dao.getByUid(uid)
        if (existing == null) {
            val entity = UserEntity(
                uid = uid,
                nickname = nickname,
                avatarUrl = avatarUrl,
                relationshipState = relationshipState,
                firstSeenAt = now,
                lastSeenAt = now,
            )
            dao.insert(entity)
            entity.toModel()
        } else {
            dao.touch(uid, nickname, avatarUrl, now, relationshipState)
            existing.copy(
                nickname = nickname,
                avatarUrl = avatarUrl,
                lastSeenAt = now,
                relationshipState = relationshipState,
            ).toModel()
        }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) { dao.count() }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    private fun UserEntity.toModel() = User(
        uid = uid,
        nickname = nickname,
        avatarUrl = avatarUrl,
        relationshipState = relationshipState,
        firstSeenAt = firstSeenAt,
        lastSeenAt = lastSeenAt,
        lastActionAt = lastActionAt,
    )
}
