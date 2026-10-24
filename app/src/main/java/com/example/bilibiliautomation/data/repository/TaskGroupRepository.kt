package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.TargetDao
import com.example.bilibiliautomation.data.db.dao.TaskGroupDao
import com.example.bilibiliautomation.data.db.entity.TaskGroupEntity
import com.example.bilibiliautomation.data.db.entity.TaskGroupMemberEntity
import com.example.bilibiliautomation.domain.model.Target
import com.example.bilibiliautomation.domain.model.TaskGroup
import com.example.bilibiliautomation.domain.model.TaskGroupDetail
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class TaskGroupRepository(
    private val groupDao: TaskGroupDao,
    private val targetDao: TargetDao,
) {
    fun observeDetails(): Flow<List<TaskGroupDetail>> = combine(
        groupDao.observeGroups(),
        groupDao.observeMembers(),
        targetDao.observeAll(),
    ) { groups, members, targets ->
        val byUid = targets.associateBy { it.uid }
        groups.map { group ->
            val grouped = members.filter { it.groupId == group.id }.mapNotNull { row ->
                byUid[row.targetUid]?.let { entity ->
                    Target(
                        id = entity.id,
                        uid = entity.uid,
                        nickname = entity.nickname,
                        avatarUrl = entity.avatarUrl,
                        enabled = entity.enabled,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        lastScanAt = entity.lastScanAt,
                        fansCount = entity.fansCount,
                    )
                }
            }
            TaskGroupDetail(group = group.toModel(), members = grouped)
        }
    }

    suspend fun getDetail(id: Long): TaskGroupDetail? = withContext(Dispatchers.IO) {
        val group = groupDao.getById(id) ?: return@withContext null
        val targets = targetDao.getAll().associateBy { it.uid }
        val members = groupDao.getMembers(id).mapNotNull { row ->
            targets[row.targetUid]?.let { entity ->
                Target(
                    id = entity.id,
                    uid = entity.uid,
                    nickname = entity.nickname,
                    avatarUrl = entity.avatarUrl,
                    enabled = entity.enabled,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    lastScanAt = entity.lastScanAt,
                    fansCount = entity.fansCount,
                )
            }
        }
        TaskGroupDetail(group.toModel(), members)
    }

    suspend fun create(name: String): TaskGroup = withContext(Dispatchers.IO) {
        val now = TimeMs.wallClock()
        val trimmed = name.trim().ifBlank { "未命名分组" }
        val entity = TaskGroupEntity(name = trimmed, createdAt = now, updatedAt = now)
        val id = groupDao.insert(entity)
        entity.copy(id = id).toModel()
    }

    suspend fun rename(id: Long, name: String) = withContext(Dispatchers.IO) {
        val current = groupDao.getById(id) ?: return@withContext
        val trimmed = name.trim().ifBlank { current.name }
        groupDao.update(current.copy(name = trimmed, updatedAt = TimeMs.wallClock()))
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        groupDao.deleteById(id)
    }

    suspend fun addMember(groupId: Long, targetUid: String) = withContext(Dispatchers.IO) {
        val uid = targetUid.trim()
        if (uid.isBlank() || groupDao.getById(groupId) == null) return@withContext
        val nextOrder = groupDao.maxSortOrder(groupId) + 1
        groupDao.insertMember(
            TaskGroupMemberEntity(
                groupId = groupId,
                targetUid = uid,
                sortOrder = nextOrder,
                createdAt = TimeMs.wallClock(),
            ),
        )
        groupDao.getById(groupId)?.let { groupDao.update(it.copy(updatedAt = TimeMs.wallClock())) }
    }

    suspend fun removeMember(groupId: Long, targetUid: String) = withContext(Dispatchers.IO) {
        groupDao.deleteMember(groupId, targetUid)
    }

    suspend fun removeTargetFromAll(targetUid: String) = withContext(Dispatchers.IO) {
        groupDao.deleteMembersByTarget(targetUid)
    }

    private fun TaskGroupEntity.toModel() = TaskGroup(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
