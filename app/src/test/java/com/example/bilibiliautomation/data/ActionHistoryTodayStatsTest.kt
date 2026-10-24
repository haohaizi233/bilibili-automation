package com.example.bilibiliautomation.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bilibiliautomation.data.db.AutomationDatabase
import com.example.bilibiliautomation.data.repository.ActionRepository
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ActionHistoryTodayStatsTest {
    private lateinit var db: AutomationDatabase
    private lateinit var repo: ActionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AutomationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ActionRepository(db.actionHistoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun todayStatsIgnoreYesterdayAndKeepTaskIsolationBuckets() = runBlocking {
        val yesterday = parse("2026-09-18 23:59:00.000")
        val todayMorning = parse("2026-09-19 00:00:01.000")
        val todayEvening = parse("2026-09-19 22:34:00.000")
        val dayStart = TimeMs.beijingDayStart(todayEvening)

        record("1", "FOLLOW", ResultCode.OK, yesterday)
        record("1", "FOLLOW", ResultCode.OK, todayMorning)
        record("1", "FOLLOW", ResultCode.OK, todayEvening)
        record("1", "SKIP", ResultCode.SKIPPED, todayEvening)
        record("1", "FOLLOW", ResultCode.UNKNOWN, todayEvening)
        record("2", "FOLLOW", ResultCode.OK, todayEvening)

        val stats = repo.statsSince(dayStart)
        assertEquals(3, stats.followed)
        assertEquals(1, stats.skipped)
        assertEquals(1, stats.failed)
        assertEquals(3, repo.countFollowAttemptsToday("1", todayEvening))
        assertEquals(1, repo.countFollowAttemptsToday("2", todayEvening))
        assertEquals(0, repo.countFollowAttemptsToday("1", parse("2026-09-20 00:00:00.000")))
    }

    private suspend fun record(account: String, type: String, code: String, at: Long) {
        repo.record(
            accountUid = account,
            targetUid = "u",
            sourceTargetUid = "up",
            sourceVideoId = "BV1",
            sourceCommentId = "c1",
            actionType = type,
            beforeState = null,
            afterState = code,
            resultCode = code,
            resultMessage = null,
            createdAt = at,
        )
    }

    private fun parse(text: String): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)
        fmt.timeZone = TimeMs.BEIJING
        return fmt.parse(text)!!.time
    }
}
