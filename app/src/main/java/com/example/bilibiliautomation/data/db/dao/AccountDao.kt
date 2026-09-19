package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bilibiliautomation.data.db.entity.AccountEntity
import com.example.bilibiliautomation.domain.model.AccountStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY updatedAt DESC")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AccountEntity): Long

    @Update
    suspend fun update(entity: AccountEntity)

    @Query("UPDATE accounts SET status = :status, updatedAt = :updatedAt, lastErrorCode = :errorCode, lastErrorMessage = :errorMessage, cooldownUntil = :cooldownUntil WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: AccountStatus,
        updatedAt: Long,
        errorCode: String?,
        errorMessage: String?,
        cooldownUntil: Long?,
    )

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM accounts WHERE uid LIKE 'pending-%'")
    suspend fun deletePendingPlaceholders()

    @Query("UPDATE accounts SET sessionProfileId = :sessionProfileId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSessionProfileId(id: Long, sessionProfileId: String, updatedAt: Long)

    @Query(
        """
        UPDATE accounts SET uid = :uid, nickname = :nickname, status = :status,
        updatedAt = :updatedAt, lastLoginAt = :lastLoginAt, lastValidatedAt = :lastValidatedAt,
        lastErrorCode = null, lastErrorMessage = null
        WHERE id = :id
        """,
    )
    suspend fun markValidated(
        id: Long,
        uid: String,
        nickname: String,
        status: AccountStatus,
        updatedAt: Long,
        lastLoginAt: Long,
        lastValidatedAt: Long,
    )

    @Query(
        """
        UPDATE accounts SET nickname = :nickname, avatarUrl = :avatarUrl, fansCount = :fansCount, followingCount = :followingCount, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateProfile(
        id: Long,
        nickname: String,
        avatarUrl: String,
        fansCount: Long,
        followingCount: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE accounts SET uid = :uid, nickname = :nickname, lastLoginAt = :lastLoginAt,
        lastValidatedAt = :lastValidatedAt, updatedAt = :updatedAt,
        lastErrorCode = null, lastErrorMessage = null
        WHERE id = :id
        """,
    )
    suspend fun markRevalidated(
        id: Long,
        uid: String,
        nickname: String,
        lastLoginAt: Long,
        lastValidatedAt: Long,
        updatedAt: Long,
    )
}
