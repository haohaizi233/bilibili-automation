package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.TargetDao
import com.example.bilibiliautomation.data.db.dao.TaskGroupDao
import com.example.bilibiliautomation.data.db.entity.TargetEntity
import com.example.bilibiliautomation.domain.model.Target
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TargetRepository(
    private val dao: TargetDao,
    private val groupDao: TaskGroupDao? = null,
) {
    fun observeAll(): Flow<List<Target>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getByUid(uid: String): Target? = withContext(Dispatchers.IO) {
        dao.getByUid(uid)?.toModel()
    }

    suspend fun getById(id: Long): Target? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toModel()
    }

    suspend fun getAll(): List<Target> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toModel() }
    }

    suspend fun upsert(
        uid: String,
        nickname: String = "",
        avatarUrl: String = "",
        fansCount: Long = -1L,
    ): Target =
        withContext(Dispatchers.IO) {
            val existing = dao.getByUid(uid)
            val now = TimeMs.wallClock()
            if (existing != null) {
                val updated = existing.copy(
                    nickname = nickname.ifBlank { existing.nickname },
                    avatarUrl = avatarUrl.ifBlank { existing.avatarUrl },
                    fansCount = if (fansCount >= 0L) fansCount else existing.fansCount,
                    updatedAt = now,
                )
                dao.update(updated)
                updated.toModel()
            } else {
                val entity = TargetEntity(
                    uid = uid,
                    nickname = nickname,
                    avatarUrl = avatarUrl,
                    enabled = true,
                    createdAt = now,
                    updatedAt = now,
                    fansCount = fansCount,
                )
                val id = dao.insert(entity)
                if (id == -1L) {
                    dao.getByUid(uid)!!.toModel()
                } else {
                    entity.copy(id = id).toModel()
                }
            }
        }

    suspend fun setEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.getById(id) ?: return@withContext
        dao.update(current.copy(enabled = enabled, updatedAt = TimeMs.wallClock()))
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { groupDao?.deleteMembersByTarget(it.uid) }
        dao.deleteById(id)
    }

    suspend fun deleteByUid(uid: String) = withContext(Dispatchers.IO) {
        groupDao?.deleteMembersByTarget(uid)
        dao.getByUid(uid)?.let { dao.deleteById(it.id) }
    }

    private fun TargetEntity.toModel() = Target(
        id = id,
        uid = uid,
        nickname = nickname,
        avatarUrl = avatarUrl,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastScanAt = lastScanAt,
        fansCount = fansCount,
    )
}
