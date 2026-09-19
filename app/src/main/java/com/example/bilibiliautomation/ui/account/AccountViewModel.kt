package com.example.bilibiliautomation.ui.account

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibiliautomation.app.AppContainer
import com.example.bilibiliautomation.data.api.qr.QrBitmapEncoder
import com.example.bilibiliautomation.data.api.qr.QrLoginPhase
import com.example.bilibiliautomation.data.api.qr.QrLoginUiState
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AccountSession
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val accounts: StateFlow<List<Account>> = container.accountManager.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentAccountId: StateFlow<Long?> = container.sessionManager.currentAccountIdFlow

    val qrLogin: StateFlow<QrLoginUiState> = container.qrLoginRepository.state

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    private var pollJob: Job? = null
    private var pendingAccountId: Long? = null
    private var encodedUrl: String? = null

    fun currentHttpSession(): AccountSession? = container.apiSessionManager.current()

    fun add(uid: String, nickname: String) {
        if (uid.isBlank()) return
        viewModelScope.launch { container.accountManager.addAccount(uid.trim(), nickname.trim()) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { container.accountManager.deleteAccount(id) }
    }

    fun setCurrent(id: Long) {
        viewModelScope.launch {
            container.accountManager.setCurrentAccount(id)
            container.apiSessionManager.activate(id)
        }
    }

    fun prepareQrLogin() {
        pollJob?.cancel()
        pollJob = null
        encodedUrl = null
        _qrBitmap.value = null
        container.qrLoginRepository.reset()
        cleanupAbandonedPending()
    }

    fun startQrLogin(accountId: Long) {
        beginQrSession(accountId)
    }

    fun startNewQrLogin() {
        beginQrSession(null)
    }

    fun refreshQr() {
        beginQrSession(pendingAccountId)
    }

    fun stopQrLogin() {
        pollJob?.cancel()
        pollJob = null
        cleanupAbandonedPending()
    }

    private fun cleanupAbandonedPending() {
        val pendingId = pendingAccountId ?: return
        pendingAccountId = null
        viewModelScope.launch {
            // Only delete if it's still a placeholder (login never validated it).
            val account = container.accountRepository.getById(pendingId) ?: return@launch
            if (account.uid.startsWith("pending-")) {
                container.accountRepository.delete(pendingId)
            }
        }
    }

    fun validate(accountId: Long) {
        viewModelScope.launch {
            container.biliApiProvider.checkSession(accountId)
            container.userProfileSync.refreshAccount(accountId, force = true)
            container.userProfileSync.refreshStats(accountId)
        }
    }

    fun refreshProfiles() {
        viewModelScope.launch { container.userProfileSync.refreshAccounts() }
    }

    init {
        refreshProfiles()
    }

    private fun beginQrSession(existingAccountId: Long?) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val accountId = existingAccountId
                    ?: pendingAccountId
                    ?: container.accountManager.addAccount("pending-${TimeMs.wallClock()}", "").id
                pendingAccountId = accountId
                encodedUrl = null
                _qrBitmap.value = null
                val started = container.qrLoginRepository.start(accountId)
                encodeIfNeeded(started.qrUrl)
                val key = started.qrcodeKey ?: return@launch
                container.qrLoginRepository.pollUntilTerminal(accountId, key)
                if (container.qrLoginRepository.state.value.phase == QrLoginPhase.SUCCESS) {
                    pendingAccountId = null
                    // After a successful scan, the validated uid may belong to an
                    // already-stored account (re-login of the same Bilibili user).
                    // ApiSessionManager.validate merges the pending duplicate into
                    // that existing account, so resolve the final id by uid —
                    // `accountId` (the pending row) may already be deleted.
                    val result = container.qrLoginRepository.state.value
                    val finalAccountId = result.uid?.toString()
                        ?.let { container.accountRepository.getByUid(it)?.id }
                        ?: accountId
                    container.sessionManager.markCurrent(finalAccountId)
                    container.userProfileSync.refreshAccount(finalAccountId, force = true)
                    container.userProfileSync.refreshStats(finalAccountId)
                }
            } catch (error: Throwable) {
                // CancellationException must be re-thrown to honour coroutine
                // cancellation; everything else (including Error/OOM) is surfaced
                // as a FAILED qr phase so the process never crashes from the
                // login flow itself.
                if (error is kotlinx.coroutines.CancellationException) throw error
                com.example.bilibiliautomation.logging.AutomationLogger.e("qr login session crashed", error)
                container.qrLoginRepository.markFailed("登录流程异常：${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    private suspend fun encodeIfNeeded(url: String?) {
        if (url.isNullOrBlank() || url == encodedUrl) return
        encodedUrl = url
        _qrBitmap.value = withContext(Dispatchers.Default) {
            try {
                QrBitmapEncoder.encode(url)
            } catch (error: Exception) {
                com.example.bilibiliautomation.logging.AutomationLogger.w("qr encode failed ${error.javaClass.simpleName} ${error.message}")
                null
            }
        }
    }
}
