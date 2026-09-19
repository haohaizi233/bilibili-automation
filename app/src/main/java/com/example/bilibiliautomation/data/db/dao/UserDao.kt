package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bilibiliautomation.data.db.entity.UserEntity
import com.example.bilibiliautomation.domain.model.RelationshipState

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: UserEntity): Long

    @Query(
        "UPDATE users SET nickname = :nickname, avatarUrl = :avatarUrl, lastSeenAt = :lastSeenAt, relationshipState = :relationshipState WHERE uid = :uid",
    )
    suspend fun touch(
        uid: String,
        nickname: String,
        avatarUrl: String,
        lastSeenAt: Long,
        relationshipState: RelationshipState,
    )

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}
