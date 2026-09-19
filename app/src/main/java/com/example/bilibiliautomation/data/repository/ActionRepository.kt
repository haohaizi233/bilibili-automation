package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.ActionHistoryDao
import com.example.bilibiliautomation.data.db.entity.ActionHistoryEntity
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ActionRepository(
    private val dao: ActionHistoryDao,
) {
    fun observeRecent(limit: Int = 100): Flow<List<ActionHistoryEntity>> = dao.observeRecent(limit)

    suspend fun record(
        accountUid: String,
        targetUid: String?,
        sourceTargetUid: String?,
        sourceVideoId: String?,
        sourceCommentId: String?,
        actionType: String,
        beforeState: String?,
        afterState: String?,
        resultCode: String,
        resultMessage: String?,
    ) = withContext(Dispatchers.IO) {
        dao.insert(
            ActionHistoryEntity(
                accountUid = accountUid,
                targetUid = targetUid,
                sourceTargetUid = sourceTargetUid,
                sourceVideoId = sourceVideoId,
                sourceCommentId = sourceCommentId,
                actionType = actionType,
                beforeState = beforeState,
                afterState = afterState,
                resultCode = resultCode,
                resultMessage = resultMessage,
                createdAt = TimeMs.wallClock(),
            ),
        )
    }
}
