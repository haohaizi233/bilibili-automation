package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.AccountDao
import com.example.bilibiliautomation.data.db.entity.AccountEntity
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class AccountRepository(
    private val dao: AccountDao,
) {
    fun observeAll(): Flow<List<Account>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getAll(): List<Account> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toModel() }
    }

    suspend fun getById(id: Long): Account? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toModel()
    }

    suspend fun getByUid(uid: String): Account? = withContext(Dispatchers.IO) {
        dao.getByUid(uid)?.toModel()
    }

    suspend fun add(
        uid: String,
        nickname: String = "",
        avatarUrl: String = "",
    ): Account = withContext(Dispatchers.IO) {
        val now = TimeMs.wallClock()
        val entity = AccountEntity(
            uid = uid,
            nickname = nickname,
            avatarUrl = avatarUrl,
            status = AccountStatus.LOGIN_REQUIRED,
            createdAt = now,
            updatedAt = now,
            sessionProfileId = UUID.randomUUID().toString(),
        )
        val id = dao.insert(entity)
        entity.copy(id = id).toModel()
    }

    suspend fun update(account: Account) = withContext(Dispatchers.IO) {
        dao.update(account.toEntity())
    }

    suspend fun markStatus(
        id: Long,
        status: AccountStatus,
        errorCode: String? = null,
        errorMessage: String? = null,
        cooldownUntil: Long? = null,
    ) = withContext(Dispatchers.IO) {
        dao.updateStatus(id, status, TimeMs.wallClock(), errorCode, errorMessage, cooldownUntil)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deletePendingPlaceholders() = withContext(Dispatchers.IO) {
        dao.deletePendingPlaceholders()
    }

    suspend fun updateSessionProfileId(id: Long, sessionProfileId: String) = withContext(Dispatchers.IO) {
        dao.updateSessionProfileId(id, sessionProfileId, TimeMs.wallClock())
    }

    suspend fun markValidated(id: Long, uid: String, nickname: String) = withContext(Dispatchers.IO) {
        val now = TimeMs.wallClock()
        dao.markValidated(
            id = id,
            uid = uid,
            nickname = nickname,
            status = AccountStatus.ACTIVE,
            updatedAt = now,
            lastLoginAt = now,
            lastValidatedAt = now,
        )
    }

    suspend fun markRevalidated(id: Long, uid: String, nickname: String) = withContext(Dispatchers.IO) {
        val now = TimeMs.wallClock()
        dao.markRevalidated(
            id = id,
            uid = uid,
            nickname = nickname,
            lastLoginAt = now,
            lastValidatedAt = now,
            updatedAt = now,
        )
    }

    suspend fun updateProfile(
        id: Long,
        nickname: String? = null,
        avatarUrl: String? = null,
        fansCount: Long? = null,
        followingCount: Long? = null,
    ) = withContext(Dispatchers.IO) {
        val current = dao.getById(id) ?: return@withContext
        dao.updateProfile(
            id = id,
            nickname = nickname?.ifBlank { current.nickname } ?: current.nickname,
            avatarUrl = avatarUrl?.ifBlank { current.avatarUrl } ?: current.avatarUrl,
            fansCount = fansCount ?: current.fansCount,
            followingCount = followingCount ?: current.followingCount,
            updatedAt = TimeMs.wallClock(),
        )
    }

    private fun AccountEntity.toModel() = Account(
        id = id,
        uid = uid,
        nickname = nickname,
        avatarUrl = avatarUrl,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastLoginAt = lastLoginAt,
        lastValidatedAt = lastValidatedAt,
        cooldownUntil = cooldownUntil,
        disabled = disabled,
        lastErrorCode = lastErrorCode,
        lastErrorMessage = lastErrorMessage,
        sessionProfileId = sessionProfileId,
        fansCount = fansCount,
        followingCount = followingCount,
    )

    private fun Account.toEntity() = AccountEntity(
        id = id,
        uid = uid,
        nickname = nickname,
        avatarUrl = avatarUrl,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastLoginAt = lastLoginAt,
        lastValidatedAt = lastValidatedAt,
        cooldownUntil = cooldownUntil,
        disabled = disabled,
        lastErrorCode = lastErrorCode,
        lastErrorMessage = lastErrorMessage,
        sessionProfileId = sessionProfileId,
        fansCount = fansCount,
        followingCount = followingCount,
    )
}
