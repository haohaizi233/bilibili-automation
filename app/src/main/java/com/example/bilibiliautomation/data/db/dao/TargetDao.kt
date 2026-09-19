package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bilibiliautomation.data.db.entity.TargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDao {
    @Query("SELECT * FROM targets ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TargetEntity>>

    @Query("SELECT * FROM targets ORDER BY updatedAt DESC")
    suspend fun getAll(): List<TargetEntity>

    @Query("SELECT * FROM targets WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): TargetEntity?

    @Query("SELECT * FROM targets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TargetEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TargetEntity): Long

    @Update
    suspend fun update(entity: TargetEntity)

    @Query("DELETE FROM targets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
