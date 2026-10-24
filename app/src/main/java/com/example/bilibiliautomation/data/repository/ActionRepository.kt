package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.db.dao.ActionHistoryDao
import com.example.bilibiliautomation.data.db.entity.ActionHistoryEntity
import com.example.bilibiliautomation.domain.model.TodayStats
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ActionRepository(
    private val dao: ActionHistoryDao,
) {
    fun observeRecent(limit: Int = 100): Flow<List<ActionHistoryEntity>> = dao.observeRecent(limit)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTodayStats(): Flow<TodayStats> = beijingDayStartTicks()
        .flatMapLatest { start ->
            dao.observeStatsSince(start).map { it ?: TodayStats() }
        }

    suspend fun statsSince(since: Long): TodayStats = withContext(Dispatchers.IO) {
        dao.getStatsSince(since) ?: TodayStats()
    }

    suspend fun countFollowAttemptsToday(accountUid: String, now: Long = TimeMs.wallClock()): Int =
        withContext(Dispatchers.IO) {
            dao.countFollowAttemptsSince(TimeMs.beijingDayStart(now), accountUid)
        }

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
        createdAt: Long = TimeMs.wallClock(),
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
                createdAt = createdAt,
            ),
        )
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    private fun beijingDayStartTicks(): Flow<Long> = flow {
        var lastStart = -1L
        while (true) {
            val start = TimeMs.beijingDayStart()
            if (start != lastStart) {
                lastStart = start
                emit(start)
            }
            delay(TimeMs.beijingDayPollIntervalMs())
        }
    }
}
