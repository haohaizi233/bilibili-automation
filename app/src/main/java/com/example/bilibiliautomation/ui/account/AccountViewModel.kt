package com.example.bilibiliautomation.ui.account

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibiliautomation.app.AppContainer
import com.example.bilibiliautomation.data.api.qr.QrBitmapEncoder
import com.example.bilibiliautomation.data.api.qr.QrLoginPhase
import com.example.bilibiliautomation.data.api.qr.QrLoginUiState
import com.example.bilibiliautomation.data.api.sms.SmsLoginPhase
import com.example.bilibiliautomation.data.api.sms.SmsLoginUiState
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.SessionCookie
import com.example.bilibiliautomation.domain.model.AccountSession
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val smsLogin: StateFlow<SmsLoginUiState> = container.smsLoginRepository.state

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    private var pollJob: Job? = null
    private var hitlTimeoutJob: Job? = null
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

    fun prepareSmsLogin(existingAccountId: Long? = null) {
        pollJob?.cancel()
        pollJob = null
        cancelHitlTimeout()
        container.smsLoginRepository.reset()
        if (existingAccountId != null) pendingAccountId = existingAccountId
        beginSmsSession()
    }

    fun stopSmsLogin() {
        pollJob?.cancel()
        pollJob = null
        cancelHitlTimeout()
        cleanupAbandonedPending()
        container.smsLoginRepository.reset()
    }

    fun requestSmsCode(phone: String) {
        val accountId = pendingAccountId ?: return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val next = container.smsLoginRepository.beginCaptcha(accountId, phone)
                if (next.phase == SmsLoginPhase.WAITING_GEEST || next.phase == SmsLoginPhase.HITL_WEBVIEW) {
                    armHitlTimeout()
                }
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                container.smsLoginRepository.markFailed("获取验证码失败：${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    fun onGeeSuccess(challenge: String, validate: String, seccode: String) {
        val accountId = pendingAccountId ?: return
        cancelHitlTimeout()
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val next = container.smsLoginRepository.sendAfterGee(accountId, challenge, validate, seccode)
                if (next.phase == SmsLoginPhase.HITL_WEBVIEW) armHitlTimeout()
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                container.smsLoginRepository.markFailed("发送短信失败：${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    fun onGeeClosed() {
        cancelHitlTimeout()
        container.smsLoginRepository.cancelGee()
    }

    fun onOfficialSmsSent(captchaKey: String) {
        cancelHitlTimeout()
        container.smsLoginRepository.markOfficialSmsSent(captchaKey)
    }

    fun markWebViewLoggingIn() {
        container.smsLoginRepository.markLoggingIn()
    }

    fun openOfficialHitl() {
        pendingAccountId ?: return
        container.smsLoginRepository.openHitl()
        armHitlTimeout()
    }

    fun onHitlClosed() {
        cancelHitlTimeout()
        container.smsLoginRepository.cancelHitl()
    }

    fun onHitlCookies(cookies: List<SessionCookie>) {
        val accountId = pendingAccountId ?: return
        cancelHitlTimeout()
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val result = container.smsLoginRepository.completeFromWebCookies(accountId, cookies)
                if (result.phase == SmsLoginPhase.SUCCESS) {
                    finishLogin(accountId, result.uid)
                }
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                container.smsLoginRepository.markFailed("保存登录状态失败：${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    fun onGeeError(message: String) {
        cancelHitlTimeout()
        container.smsLoginRepository.markFailed(message.ifBlank { "人机验证失败" })
    }

    fun submitSmsCode(code: String) {
        val accountId = pendingAccountId ?: return
        cancelHitlTimeout()
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val result = container.smsLoginRepository.login(accountId, code)
                when (result.phase) {
                    SmsLoginPhase.SUCCESS -> finishLogin(accountId, result.uid)
                    SmsLoginPhase.HITL_WEBVIEW -> armHitlTimeout()
                    else -> Unit
                }
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                container.smsLoginRepository.markFailed("登录失败：${error.message ?: error.javaClass.simpleName}")
            }
        }
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
                    finishLogin(accountId, container.qrLoginRepository.state.value.uid)
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

    private fun beginSmsSession() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val accountId = pendingAccountId
                    ?: container.accountManager.addAccount("pending-${TimeMs.wallClock()}", "").id
                pendingAccountId = accountId
                container.smsLoginRepository.prepare(accountId)
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                container.smsLoginRepository.markFailed("准备登录失败：${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    private suspend fun finishLogin(accountId: Long, uid: Long?) {
        cancelHitlTimeout()
        pendingAccountId = null
        val finalAccountId = uid?.toString()
            ?.let { container.accountRepository.getByUid(it)?.id }
            ?: accountId
        container.sessionManager.markCurrent(finalAccountId)
        container.userProfileSync.refreshAccount(finalAccountId, force = true)
        container.userProfileSync.refreshStats(finalAccountId)
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

    private fun armHitlTimeout() {
        hitlTimeoutJob?.cancel()
        hitlTimeoutJob = viewModelScope.launch {
            delay(10 * 60 * 1000L)
            val phase = container.smsLoginRepository.state.value.phase
            if (phase == SmsLoginPhase.WAITING_GEEST || phase == SmsLoginPhase.HITL_WEBVIEW) {
                container.smsLoginRepository.markFailed("人机验证超时（10 分钟无人操作）")
            }
        }
    }

    private fun cancelHitlTimeout() {
        hitlTimeoutJob?.cancel()
        hitlTimeoutJob = null
    }
}
