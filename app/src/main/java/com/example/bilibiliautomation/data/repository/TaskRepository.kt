package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.TaskDao
import com.example.bilibiliautomation.data.db.entity.TaskCheckpointEntity
import com.example.bilibiliautomation.data.db.entity.TaskEntity
import com.example.bilibiliautomation.domain.model.Task
import com.example.bilibiliautomation.domain.model.TaskCheckpoint
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.domain.workflow.WorkflowState
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepository(
    private val dao: TaskDao,
) {
    fun observeAll(): Flow<List<Task>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getById(id: Long): Task? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toModel()
    }

    suspend fun create(
        targetUid: String?,
        accountId: Long?,
        groupId: Long? = null,
        groupIndex: Int = 0,
    ): Task = withContext(Dispatchers.IO) {
        val now = TimeMs.wallClock()
        val entity = TaskEntity(
            status = TaskStatus.QUEUED,
            targetUid = targetUid,
            accountId = accountId,
            groupId = groupId,
            groupIndex = groupIndex,
            createdAt = now,
        )
        val id = dao.insert(entity)
        entity.copy(id = id).toModel()
    }

    suspend fun updateStatus(
        id: Long,
        status: TaskStatus,
        lastError: String? = null,
        startedAt: Long? = null,
        pausedAt: Long? = null,
        finishedAt: Long? = null,
    ) = withContext(Dispatchers.IO) {
        dao.updateRuntime(id, status, lastError, pausedAt, finishedAt, startedAt)
    }

    suspend fun findDanglingRuntime(): List<Task> = withContext(Dispatchers.IO) {
        dao.getByStatuses(listOf(TaskStatus.RUNNING, TaskStatus.QUEUED)).map { it.toModel() }
    }

    suspend fun updateAccount(id: Long, accountId: Long?) = withContext(Dispatchers.IO) {
        dao.updateAccount(id, accountId)
    }

    suspend fun getLatest(): Task? = withContext(Dispatchers.IO) {
        dao.getLatest()?.toModel()
    }

    suspend fun saveCheckpoint(checkpoint: TaskCheckpoint) = withContext(Dispatchers.IO) {
        dao.upsertCheckpoint(
            TaskCheckpointEntity(
                taskId = checkpoint.taskId,
                workflowState = checkpoint.workflowState.name,
                targetUid = checkpoint.targetUid,
                videoId = checkpoint.videoId,
                videoIndex = checkpoint.videoIndex,
                commentId = checkpoint.commentId,
                commentCursor = checkpoint.commentCursor,
                commentIndex = checkpoint.commentIndex,
                currentUserUid = checkpoint.currentUserUid,
                currentUrl = checkpoint.currentUrl,
                followed = checkpoint.followed,
                skipped = checkpoint.skipped,
                failed = checkpoint.failed,
                videoTotal = checkpoint.videoTotal,
                commentTotal = checkpoint.commentTotal,
                currentVideoTitle = checkpoint.currentVideoTitle,
                currentVideoCover = checkpoint.currentVideoCover,
                updatedAt = checkpoint.updatedAt,
            ),
        )
    }

    suspend fun getCheckpoint(taskId: Long): TaskCheckpoint? = withContext(Dispatchers.IO) {
        dao.getCheckpoint(taskId)?.toModel()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAllCheckpoints()
        dao.deleteAll()
    }

    private fun TaskEntity.toModel() = Task(
        id = id,
        status = status,
        targetUid = targetUid,
        accountId = accountId,
        groupId = groupId,
        groupIndex = groupIndex,
        createdAt = createdAt,
        startedAt = startedAt,
        pausedAt = pausedAt,
        finishedAt = finishedAt,
        lastError = lastError,
    )

    private fun TaskCheckpointEntity.toModel() = TaskCheckpoint(
        taskId = taskId,
        workflowState = runCatching { WorkflowState.valueOf(workflowState) }.getOrDefault(WorkflowState.IDLE),
        targetUid = targetUid,
        videoId = videoId,
        videoIndex = videoIndex,
        commentId = commentId,
        commentCursor = commentCursor,
        commentIndex = commentIndex,
        currentUserUid = currentUserUid,
        currentUrl = currentUrl,
        followed = followed,
        skipped = skipped,
        failed = failed,
        videoTotal = videoTotal,
        commentTotal = commentTotal,
        currentVideoTitle = currentVideoTitle,
        currentVideoCover = currentVideoCover,
        updatedAt = updatedAt,
    )
}
