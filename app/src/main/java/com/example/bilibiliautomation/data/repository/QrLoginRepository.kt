package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.api.BiliApiProvider
import com.example.bilibiliautomation.data.api.dto.QrPollCode
import com.example.bilibiliautomation.data.api.qr.QrLoginPhase
import com.example.bilibiliautomation.data.api.qr.QrLoginUiState
import com.example.bilibiliautomation.data.api.session.ApiSessionManager
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.SessionState
import com.example.bilibiliautomation.logging.AutomationLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class QrLoginRepository(
    private val api: BiliApiProvider,
    private val sessions: ApiSessionManager,
    private val logger: AutomationLogger = AutomationLogger,
    private val pollIntervalMs: Long = 1_500L,
) {
    private val _state = MutableStateFlow(QrLoginUiState())
    val state: StateFlow<QrLoginUiState> = _state.asStateFlow()

    fun reset() {
        _state.value = QrLoginUiState()
    }

    /**
     * Surface an unrecoverable error from the caller (e.g. an exception in
     * the ViewModel launch block) as a FAILED qr phase, instead of letting
     * it escape and crash the process.
     */
    fun markFailed(message: String) {
        _state.value = QrLoginUiState(phase = QrLoginPhase.FAILED, message = message)
        logger.w("qr login markFailed message=$message")
    }

    suspend fun start(accountId: Long): QrLoginUiState {
        _state.value = QrLoginUiState(phase = QrLoginPhase.FETCHING_QR, accountId = accountId)
        sessions.activate(accountId)
        return when (val qr = api.getLoginQr(accountId)) {
            is ApiResult.Err -> {
                val next = QrLoginUiState(phase = QrLoginPhase.FAILED, accountId = accountId, message = qr.error.message)
                _state.value = next
                next
            }
            is ApiResult.Ok -> {
                val next = QrLoginUiState(
                    phase = QrLoginPhase.WAITING_SCAN,
                    accountId = accountId,
                    qrUrl = qr.value.url,
                    qrcodeKey = qr.value.qrcodeKey,
                )
                _state.value = next
                logger.i("qr generated accountId=$accountId")
                next
            }
        }
    }

    suspend fun pollUntilTerminal(accountId: Long, qrcodeKey: String): QrLoginUiState {
        while (coroutineContext.isActive) {
            val snapshot = pollOnce(accountId, qrcodeKey)
            if (snapshot.phase == QrLoginPhase.SUCCESS ||
                snapshot.phase == QrLoginPhase.EXPIRED ||
                snapshot.phase == QrLoginPhase.FAILED
            ) {
                return snapshot
            }
            delay(pollIntervalMs)
        }
        return _state.value.copy(phase = QrLoginPhase.FAILED, message = "cancelled")
    }

    suspend fun pollOnce(accountId: Long, qrcodeKey: String): QrLoginUiState {
        return when (val poll = api.pollLogin(accountId, qrcodeKey)) {
            is ApiResult.Err -> fail(accountId, poll.error.message)
            is ApiResult.Ok -> when (poll.value.mapped) {
                QrPollCode.WAITING_SCAN ->
                    emit(accountId, QrLoginPhase.WAITING_SCAN, qrcodeKey)
                QrPollCode.SCANNED ->
                    emit(accountId, QrLoginPhase.SCANNED, qrcodeKey, "scanned, waiting confirm")
                        .copy(phase = QrLoginPhase.WAITING_CONFIRM)
                        .also { _state.value = it }
                QrPollCode.EXPIRED -> fail(accountId, "qr expired", QrLoginPhase.EXPIRED)
                QrPollCode.FAILED, QrPollCode.UNKNOWN -> fail(accountId, poll.value.message.ifBlank { "qr failed" })
                QrPollCode.SUCCESS -> complete(accountId)
            }
        }
    }

    private suspend fun complete(accountId: Long): QrLoginUiState {
        _state.value = _state.value.copy(phase = QrLoginPhase.WAITING_CONFIRM, message = "validating session")
        return when (val session = api.checkSession(accountId)) {
            is ApiResult.Err -> fail(accountId, session.error.message)
            is ApiResult.Ok -> when (val state = session.value) {
                is SessionState.Valid -> {
                    val next = QrLoginUiState(
                        phase = QrLoginPhase.SUCCESS,
                        accountId = accountId,
                        uid = state.uid,
                        uname = state.uname,
                        face = state.face,
                        message = "login success",
                    )
                    _state.value = next
                    logger.i("qr login validated accountId=$accountId uid=${state.uid}")
                    next
                }
                else -> fail(accountId, "login cookies captured but session invalid: $state")
            }
        }
    }

    private fun emit(
        accountId: Long,
        phase: QrLoginPhase,
        qrcodeKey: String,
        message: String = "",
    ): QrLoginUiState {
        val next = _state.value.copy(
            accountId = accountId,
            phase = phase,
            qrcodeKey = qrcodeKey,
            message = message,
        )
        _state.value = next
        return next
    }

    private fun fail(accountId: Long, message: String, phase: QrLoginPhase = QrLoginPhase.FAILED): QrLoginUiState {
        val next = QrLoginUiState(phase = phase, accountId = accountId, message = message)
        _state.value = next
        logger.w("qr login $phase accountId=$accountId")
        return next
    }
}
