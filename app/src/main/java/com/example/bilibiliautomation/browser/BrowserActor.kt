package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.logging.AutomationLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun interface BrowserCommandHandler {
    suspend fun handle(command: BrowserCommand): AutomationResult<*>
}

/**
 * Single-consumer command queue. All WebView mutations must go through [submit].
 *
 * The consumer runs off the main thread; handlers hop to Main only for actual
 * WebView API calls so navigation waits do not pin the UI thread.
 */
class BrowserActor(
    private val handler: BrowserCommandHandler,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val logger: AutomationLogger = AutomationLogger,
    parentJob: Job? = null,
) {
    private val job = SupervisorJob(parentJob)
    private val scope = CoroutineScope(job + Dispatchers.Default)
    private val commands = Channel<BrowserCommand>(Channel.UNLIMITED)
    private val running = AtomicBoolean(true)
    private val sequence = AtomicInteger(0)

    @Volatile
    private var currentJob: Job? = null

    val processedCount: Int get() = sequence.get()

    init {
        scope.launch {
            for (command in commands) {
                if (command.deferred.isCompleted) continue
                val child = launch {
                    val result = try {
                        handler.handle(command)
                    } catch (cancelled: CancellationException) {
                        AutomationResult.cancelled<Unit>(cancelled.message ?: "cancelled")
                    } catch (error: Exception) {
                        logger.e("BrowserActor ${command.name} crashed", error)
                        AutomationResult.fail<Unit>(ResultCode.UNKNOWN, error.message ?: "actor error")
                    }
                    sequence.incrementAndGet()
                    if (!command.deferred.isCompleted) {
                        command.deferred.complete(result)
                    }
                }
                currentJob = child
                command.deferred.invokeOnCompletion { cause ->
                    if (cause is CancellationException) child.cancel(cause)
                }
                child.join()
                currentJob = null
            }
        }
    }

    suspend fun <T> submit(command: BrowserCommand): AutomationResult<T> {
        if (!running.get()) {
            return AutomationResult.cancelled("actor shutdown")
        }
        commands.send(command)
        return try {
            @Suppress("UNCHECKED_CAST")
            command.deferred.await() as AutomationResult<T>
        } catch (cancelled: CancellationException) {
            command.deferred.cancel()
            throw cancelled
        }
    }

    suspend fun <T> submit(
        name: String,
        block: suspend () -> AutomationResult<T>,
    ): AutomationResult<T> {
        val command = CustomCommand(name = name) { block() }
        return submit(command)
    }

    fun cancelPending() {
        currentJob?.cancel()
        while (true) {
            val queued = commands.tryReceive().getOrNull() ?: break
            if (!queued.deferred.isCompleted) {
                queued.deferred.complete(AutomationResult.cancelled<Unit>("cancelled pending"))
            }
        }
    }

    fun shutdown() {
        if (!running.compareAndSet(true, false)) return
        cancelPending()
        commands.close()
        scope.cancel()
        logger.i("BrowserActor shutdown")
    }

    suspend fun <T> onMain(block: suspend () -> T): T = withContext(mainDispatcher) { block() }
}
