package com.example.bilibiliautomation.domain.workflow

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bilibiliautomation.browser.BrowserHost
import com.example.bilibiliautomation.browser.SessionManager
import com.example.bilibiliautomation.data.db.AutomationDatabase
import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.repository.ActionRepository
import com.example.bilibiliautomation.data.repository.CommentRepository
import com.example.bilibiliautomation.data.repository.TargetRepository
import com.example.bilibiliautomation.data.repository.TaskRepository
import com.example.bilibiliautomation.data.repository.UserRepository
import com.example.bilibiliautomation.data.repository.VideoRepository
import com.example.bilibiliautomation.data.secure.MemorySessionBlobStore
import com.example.bilibiliautomation.data.secure.SecureSessionStore
import com.example.bilibiliautomation.domain.account.AccountManager
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.RelationshipState
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.domain.model.Task
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.event.AutomationEventBus
import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class WorkflowEngineTest {
    private lateinit var db: AutomationDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var tasks: TaskRepository
    private lateinit var users: UserRepository
    private lateinit var accountManager: AccountManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AutomationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accounts = AccountRepository(db.accountDao())
        tasks = TaskRepository(db.taskDao())
        users = UserRepository(db.userDao())
        val store = SecureSessionStore(MemorySessionBlobStore())
        val host = BrowserHost(context)
        val sessions = SessionManager(context, store, accounts, host)
        accountManager = AccountManager(accounts, store, sessions)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mockWorkflowWalksExpectedStatesAndSkipsUnknown() = runBlocking {
        val adapter = FakeSiteAdapter()
        adapter.setUnknownRelationship()
        val engine = engine(adapter)
        val task = seedTask()
        val result = engine.run(task, operationDelay())
        assertTrue(result.success)
        assertEquals(WorkflowState.FINISHED, engine.state.value)
        assertTrue(adapter.actions.contains("VALIDATE_ACCOUNT"))
        assertTrue(adapter.actions.contains("OPEN_TARGET"))
        assertTrue(adapter.actions.contains("LOAD_VIDEOS"))
        assertTrue(adapter.actions.any { it.startsWith("OPEN_VIDEO") })
        assertTrue(adapter.actions.contains("LOAD_COMMENTS"))
        assertTrue(adapter.actions.any { it.startsWith("OPEN_USER") })
        assertTrue(adapter.actions.any { it.startsWith("CHECK_RELATIONSHIP") })
        assertFalse("UNKNOWN must skip action", adapter.actions.contains("PERFORM_ACTION"))
        assertEquals(RelationshipState.UNKNOWN, users.getByUid("30001")?.relationshipState)
        val checkpoint = tasks.getCheckpoint(task.id)
        assertEquals(WorkflowState.FINISHED, checkpoint?.workflowState)
    }

    @Test
    fun rateLimitEntersCooldownWithoutSwitchingAccount() = runBlocking {
        val first = accounts.add("10001", "a")
        val second = accounts.add("10002", "b")
        val adapter = FakeSiteAdapter(restrictCode = ResultCode.RATE_LIMITED)
        val engine = engine(adapter)
        val task = tasks.create("20002", first.id)
        val result = engine.run(task.copy(status = TaskStatus.RUNNING), operationDelay())
        assertEquals(ResultCode.RATE_LIMITED, result.code)
        assertEquals(WorkflowState.COOLDOWN, engine.state.value)
        assertEquals(AccountStatus.COOLDOWN, accounts.getById(first.id)?.status)
        assertEquals(AccountStatus.LOGIN_REQUIRED, accounts.getById(second.id)?.status)
        assertFalse(adapter.actions.contains("PERFORM_ACTION"))
    }

    private fun engine(adapter: FakeSiteAdapter) = WorkflowEngine(
        adapter = adapter,
        taskRepository = tasks,
        accountRepository = accounts,
        videoRepository = VideoRepository(db.videoDao()),
        commentRepository = CommentRepository(db.commentDao()),
        userRepository = users,
        actionRepository = ActionRepository(db.actionHistoryDao()),
        accountManager = accountManager,
        eventBus = AutomationEventBus(),
        operationIntervalMs = 0L,
    )

    private suspend fun seedTask(): Task {
        val account = accounts.add("10001", "mock")
        TargetRepository(db.targetDao()).upsert("20002", "mock-target")
        return tasks.create("20002", account.id).copy(status = TaskStatus.RUNNING)
    }

    private fun operationDelay(): WorkflowState = WorkflowState.VALIDATE_ACCOUNT
}
