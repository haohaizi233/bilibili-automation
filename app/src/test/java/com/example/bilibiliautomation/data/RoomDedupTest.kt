package com.example.bilibiliautomation.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bilibiliautomation.data.db.AutomationDatabase
import com.example.bilibiliautomation.data.db.entity.CommentEntity
import com.example.bilibiliautomation.data.db.entity.TargetEntity
import com.example.bilibiliautomation.data.db.entity.UserEntity
import com.example.bilibiliautomation.data.db.entity.VideoEntity
import com.example.bilibiliautomation.domain.model.RelationshipState
import kotlinx.coroutines.runBlocking
import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class RoomDedupTest {
    private lateinit var db: AutomationDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AutomationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun duplicateUserUidDoesNotCreateSecondRow() = runBlocking {
        val now = 1L
        val first = UserEntity("u1", "a", "", RelationshipState.UNKNOWN, now, now)
        val second = UserEntity("u1", "b", "", RelationshipState.NOT_FOLLOWING, now, now)
        db.userDao().insert(first)
        db.userDao().insert(second)
        assertEquals(1, db.userDao().count())
        assertEquals(RelationshipState.UNKNOWN, db.userDao().getByUid("u1")?.relationshipState)
    }

    @Test
    fun duplicateCommentAndVideoAndTargetAreIgnored() = runBlocking {
        val now = 1L
        db.targetDao().insert(TargetEntity(uid = "t1", nickname = "n", createdAt = now, updatedAt = now))
        db.targetDao().insert(TargetEntity(uid = "t1", nickname = "other", createdAt = now, updatedAt = now))
        assertEquals("n", db.targetDao().getByUid("t1")?.nickname)

        db.videoDao().insert(VideoEntity("BV1", "t1", "title", createdAt = now, updatedAt = now))
        db.videoDao().insert(VideoEntity("BV1", "t1", "other", createdAt = now, updatedAt = now))
        assertEquals("title", db.videoDao().getByBvid("BV1")?.title)

        db.commentDao().insert(CommentEntity("c1", "BV1", "u1", true, createdAt = now))
        db.commentDao().insert(CommentEntity("c1", "BV1", "u2", true, createdAt = now))
        assertEquals("u1", db.commentDao().getById("c1")?.userUid)
    }
}
