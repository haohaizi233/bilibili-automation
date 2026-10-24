package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bilibiliautomation.data.db.entity.VideoEntity

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE targetUid = :targetUid ORDER BY createdAt ASC")
    suspend fun getByTarget(targetUid: String): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE bvid = :bvid LIMIT 1")
    suspend fun getByBvid(bvid: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<VideoEntity>): List<Long>

    @Query("UPDATE videos SET processed = :processed, processedAt = :processedAt, processedCommentCount = :count, lastCommentCursor = :cursor, updatedAt = :updatedAt WHERE bvid = :bvid")
    suspend fun markProcessed(
        bvid: String,
        processed: Boolean,
        processedAt: Long?,
        count: Int,
        cursor: String?,
        updatedAt: Long,
    )

    @Query("DELETE FROM videos")
    suspend fun deleteAll()
}
