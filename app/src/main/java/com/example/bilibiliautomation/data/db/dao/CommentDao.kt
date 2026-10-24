package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bilibiliautomation.data.db.entity.CommentEntity

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY createdAt ASC")
    suspend fun getByVideo(videoId: String): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE commentId = :commentId LIMIT 1")
    suspend fun getById(commentId: String): CommentEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<CommentEntity>): List<Long>

    @Query("UPDATE comments SET processed = 1, processedAt = :processedAt WHERE commentId = :commentId")
    suspend fun markProcessed(commentId: String, processedAt: Long)

    @Query("DELETE FROM comments")
    suspend fun deleteAll()
}
