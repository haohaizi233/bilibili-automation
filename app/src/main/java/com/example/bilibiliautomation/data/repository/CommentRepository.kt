package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.CommentDao
import com.example.bilibiliautomation.data.db.entity.CommentEntity
import com.example.bilibiliautomation.domain.model.Comment
import com.example.bilibiliautomation.domain.model.LoadedComment
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentRepository(
    private val dao: CommentDao,
) {
    suspend fun getByVideo(videoId: String): List<Comment> = withContext(Dispatchers.IO) {
        dao.getByVideo(videoId).map { it.toModel() }
    }

    suspend fun upsertAll(videoId: String, comments: List<LoadedComment>): List<Comment> =
        withContext(Dispatchers.IO) {
            val now = TimeMs.wallClock()
            dao.insertAll(
                comments.map { item ->
                    CommentEntity(
                        commentId = item.commentId,
                        videoId = videoId,
                        userUid = item.userUid,
                        isRootComment = item.isRootComment,
                        createdAt = now,
                    )
                },
            )
            dao.getByVideo(videoId).map { it.toModel() }
        }

    suspend fun markProcessed(commentId: String) = withContext(Dispatchers.IO) {
        dao.markProcessed(commentId, TimeMs.wallClock())
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    private fun CommentEntity.toModel() = Comment(
        commentId = commentId,
        videoId = videoId,
        userUid = userUid,
        isRootComment = isRootComment,
        processed = processed,
        createdAt = createdAt,
        processedAt = processedAt,
    )
}
