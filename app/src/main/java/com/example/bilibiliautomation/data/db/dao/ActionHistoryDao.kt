package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.bilibiliautomation.data.db.entity.ActionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Insert
    suspend fun insert(entity: ActionHistoryEntity): Long

    @Query("SELECT * FROM action_history ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE accountUid = :accountUid ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getByAccount(accountUid: String, limit: Int = 100): List<ActionHistoryEntity>
}
