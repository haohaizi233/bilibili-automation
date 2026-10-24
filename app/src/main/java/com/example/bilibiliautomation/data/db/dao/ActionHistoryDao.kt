package com.example.bilibiliautomation.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.bilibiliautomation.data.db.entity.ActionHistoryEntity
import com.example.bilibiliautomation.domain.model.TodayStats
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Insert
    suspend fun insert(entity: ActionHistoryEntity): Long

    @Query("SELECT * FROM action_history ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE accountUid = :accountUid ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getByAccount(accountUid: String, limit: Int = 100): List<ActionHistoryEntity>

    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN resultCode = 'OK' THEN 1 ELSE 0 END), 0) AS followed,
          COALESCE(SUM(CASE WHEN resultCode = 'SKIPPED' THEN 1 ELSE 0 END), 0) AS skipped,
          COALESCE(SUM(CASE WHEN resultCode NOT IN ('OK', 'SKIPPED') THEN 1 ELSE 0 END), 0) AS failed
        FROM action_history
        WHERE createdAt >= :since
        """,
    )
    fun observeStatsSince(since: Long): Flow<TodayStats?>

    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN resultCode = 'OK' THEN 1 ELSE 0 END), 0) AS followed,
          COALESCE(SUM(CASE WHEN resultCode = 'SKIPPED' THEN 1 ELSE 0 END), 0) AS skipped,
          COALESCE(SUM(CASE WHEN resultCode NOT IN ('OK', 'SKIPPED') THEN 1 ELSE 0 END), 0) AS failed
        FROM action_history
        WHERE createdAt >= :since
        """,
    )
    suspend fun getStatsSince(since: Long): TodayStats?

    @Query(
        """
        SELECT COUNT(*) FROM action_history
        WHERE createdAt >= :since
          AND accountUid = :accountUid
          AND actionType IN ('FOLLOW', 'DRY_RUN_FOLLOW')
        """,
    )
    suspend fun countFollowAttemptsSince(since: Long, accountUid: String): Int

    @Query("DELETE FROM action_history")
    suspend fun deleteAll()
}
