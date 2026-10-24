package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bilibiliautomation.data.db.entity.TaskGroupEntity
import com.example.bilibiliautomation.data.db.entity.TaskGroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskGroupDao {
    @Query("SELECT * FROM task_groups ORDER BY updatedAt DESC")
    fun observeGroups(): Flow<List<TaskGroupEntity>>

    @Query("SELECT * FROM task_groups ORDER BY updatedAt DESC")
    suspend fun getGroups(): List<TaskGroupEntity>

    @Query("SELECT * FROM task_groups WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskGroupEntity?

    @Insert
    suspend fun insert(entity: TaskGroupEntity): Long

    @Update
    suspend fun update(entity: TaskGroupEntity)

    @Query("DELETE FROM task_groups WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM task_group_members ORDER BY sortOrder ASC, createdAt ASC")
    fun observeMembers(): Flow<List<TaskGroupMemberEntity>>

    @Query("SELECT * FROM task_group_members WHERE groupId = :groupId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getMembers(groupId: Long): List<TaskGroupMemberEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM task_group_members WHERE groupId = :groupId")
    suspend fun maxSortOrder(groupId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMember(entity: TaskGroupMemberEntity): Long

    @Query("DELETE FROM task_group_members WHERE groupId = :groupId AND targetUid = :targetUid")
    suspend fun deleteMember(groupId: Long, targetUid: String)

    @Query("DELETE FROM task_group_members WHERE targetUid = :targetUid")
    suspend fun deleteMembersByTarget(targetUid: String)
}
