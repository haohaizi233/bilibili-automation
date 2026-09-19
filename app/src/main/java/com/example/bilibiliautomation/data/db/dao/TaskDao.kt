package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bilibiliautomation.data.db.entity.TaskCheckpointEntity
import com.example.bilibiliautomation.data.db.entity.TaskEntity
import com.example.bilibiliautomation.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status IN (:statuses)")
    suspend fun getByStatuses(statuses: List<TaskStatus>): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = :status LIMIT 1")
    suspend fun getFirstByStatus(status: TaskStatus): TaskEntity?

    @Insert
    suspend fun insert(entity: TaskEntity): Long

    @Update
    suspend fun update(entity: TaskEntity)

    @Query("UPDATE tasks SET status = :status, lastError = :lastError, pausedAt = :pausedAt, finishedAt = :finishedAt, startedAt = :startedAt WHERE id = :id")
    suspend fun updateRuntime(
        id: Long,
        status: TaskStatus,
        lastError: String?,
        pausedAt: Long?,
        finishedAt: Long?,
        startedAt: Long?,
    )

    @Query("UPDATE tasks SET accountId = :accountId WHERE id = :id")
    suspend fun updateAccount(id: Long, accountId: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckpoint(entity: TaskCheckpointEntity)

    @Query("SELECT * FROM task_checkpoints WHERE taskId = :taskId LIMIT 1")
    suspend fun getCheckpoint(taskId: Long): TaskCheckpointEntity?
}
