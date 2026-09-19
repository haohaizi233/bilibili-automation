package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.VideoDao
import com.example.bilibiliautomation.data.db.entity.VideoEntity
import com.example.bilibiliautomation.domain.model.LoadedVideo
import com.example.bilibiliautomation.domain.model.Video
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(
    private val dao: VideoDao,
) {
    suspend fun getByTarget(targetUid: String): List<Video> = withContext(Dispatchers.IO) {
        dao.getByTarget(targetUid).map { it.toModel() }
    }

    suspend fun upsertAll(targetUid: String, videos: List<LoadedVideo>): List<Video> =
        withContext(Dispatchers.IO) {
            val now = TimeMs.wallClock()
            dao.insertAll(
                videos.map { item ->
                    VideoEntity(
                        bvid = item.bvid,
                        targetUid = targetUid,
                        title = item.title,
                        createdAt = now,
                        updatedAt = now,
                    )
                },
            )
            dao.getByTarget(targetUid).map { it.toModel() }
        }

    suspend fun markProcessed(bvid: String, commentCount: Int, cursor: String?) =
        withContext(Dispatchers.IO) {
            val now = TimeMs.wallClock()
            dao.markProcessed(bvid, true, now, commentCount, cursor, now)
        }

    private fun VideoEntity.toModel() = Video(
        bvid = bvid,
        targetUid = targetUid,
        title = title,
        processed = processed,
        lastCommentCursor = lastCommentCursor,
        processedCommentCount = processedCommentCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        processedAt = processedAt,
    )
}
