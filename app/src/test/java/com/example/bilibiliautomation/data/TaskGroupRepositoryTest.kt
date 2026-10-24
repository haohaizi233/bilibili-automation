package com.example.bilibiliautomation.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bilibiliautomation.data.db.AutomationDatabase
import com.example.bilibiliautomation.data.repository.TargetRepository
import com.example.bilibiliautomation.data.repository.TaskGroupRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class TaskGroupRepositoryTest {
    private lateinit var db: AutomationDatabase
    private lateinit var groups: TaskGroupRepository
    private lateinit var targets: TargetRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AutomationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        groups = TaskGroupRepository(db.taskGroupDao(), db.targetDao())
        targets = TargetRepository(db.targetDao(), db.taskGroupDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun groupKeepsMembersAndSurvivesTargetRemoval() = runBlocking {
        targets.upsert("1", "UP1")
        targets.upsert("2", "UP2")
        val group = groups.create("美食组")
        groups.addMember(group.id, "1")
        groups.addMember(group.id, "2")
        val detail = groups.getDetail(group.id)!!
        assertEquals(2, detail.memberCount)
        assertEquals(listOf("1", "2"), detail.members.map { it.uid })

        targets.deleteByUid("1")
        val afterDelete = groups.getDetail(group.id)!!
        assertEquals(listOf("2"), afterDelete.members.map { it.uid })

        groups.removeMember(group.id, "2")
        assertTrue(groups.observeDetails().first().first().members.isEmpty())
    }
}
