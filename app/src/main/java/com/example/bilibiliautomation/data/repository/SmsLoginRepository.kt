package com.example.bilibiliautomation.data.repository

import com.example.bilibiliautomation.data.api.BiliApiProvider
import com.example.bilibiliautomation.data.api.session.ApiSessionManager
import com.example.bilibiliautomation.data.api.session.WebViewCookieParser
import com.example.bilibiliautomation.data.api.sms.SmsLoginPhase
import com.example.bilibiliautomation.data.api.sms.SmsLoginUiState
import com.example.bilibiliautomation.domain.api.ApiError
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.SessionState
import com.example.bilibiliautomation.domain.model.SessionCookie
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmsLoginRepository(
    private val api: BiliApiProvider,
    private val sessions: ApiSessionManager,
    private val logger: AutomationLogger = AutomationLogger,
) {
    private val _state = MutableStateFlow(SmsLoginUiState())
    val state: StateFlow<SmsLoginUiState> = _state.asStateFlow()

    fun reset() {
        _state.value = SmsLoginUiState()
    }

    fun markFailed(message: String) {
        _state.value = _state.value.copy(phase = SmsLoginPhase.FAILED, message = message)
        logger.w("sms login failed message=$message")
    }

    suspend fun prepare(accountId: Long) {
        sessions.activate(accountId)
        val countries = when (val result = api.listSmsCountries(accountId)) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> emptyList()
        }
        val china = countries.firstOrNull { it.countryId == "86" } ?: countries.firstOrNull()
        _state.value = SmsLoginUiState(
            phase = SmsLoginPhase.IDLE,
            accountId = accountId,
            cid = china?.cid ?: 1,
            countryLabel = china?.let { "+${it.countryId} ${it.name}" } ?: "+86 中国",
            countries = countries,
            webCookies = sessions.jar(accountId).snapshot(),
        )
    }

    suspend fun beginCaptcha(accountId: Long, tel: String): SmsLoginUiState {
        val normalized = tel.filter { it.isDigit() }
        if (normalized.length !in 6..15) {
            return fail("请输入有效手机号")
        }
        _state.value = _state.value.copy(
            phase = SmsLoginPhase.LOADING_CAPTCHA,
            accountId = accountId,
            tel = normalized,
            message = "正在拉起滑块验证",
            webCookies = sessions.jar(accountId).snapshot(),
        )
        sessions.activate(accountId)
        val next = _state.value.copy(
            phase = SmsLoginPhase.WAITING_GEEST,
            webCookies = sessions.jar(accountId).snapshot(),
            message = "请在小窗中完成滑块，通过后返回填写短信验证码",
        )
        _state.value = next
        logger.i("sms slider window accountId=$accountId")
        return next
    }

    suspend fun sendAfterGee(
        accountId: Long,
        challenge: String,
        validate: String,
        seccode: String,
    ): SmsLoginUiState {
        val snapshot = _state.value
        val captcha = snapshot.captcha
        val tel = snapshot.tel
        if (captcha == null || tel.isBlank()) {
            return fail("请先获取验证码")
        }
        if (validate.isBlank()) {
            return fail("人机验证未完成")
        }
        _state.value = snapshot.copy(phase = SmsLoginPhase.SENDING, message = "正在发送短信验证码")
        return when (
            val sent = api.sendSmsCode(
                accountId = accountId,
                cid = snapshot.cid,
                tel = tel,
                token = captcha.token,
                challenge = challenge.ifBlank { captcha.challenge },
                validate = validate,
                seccode = seccode,
            )
        ) {
            is ApiResult.Err -> when (sent.error) {
                is ApiError.VerificationRequired ->
                    enterHitl(sent.error.message.ifBlank { "发送短信需要人机验证，请改用官方页面" })
                else -> fail(sent.error.message.ifBlank { "短信发送失败" })
            }
            is ApiResult.Ok -> {
                val next = snapshot.copy(
                    phase = SmsLoginPhase.CODE_SENT,
                    captchaKey = sent.value.captchaKey,
                    cooldownUntil = TimeMs.wallClock() + 60_000L,
                    captcha = null,
                    message = "验证码已发送，5 分钟内有效",
                )
                _state.value = next
                logger.i("sms code sent accountId=$accountId")
                next
            }
        }
    }

    fun markOfficialSmsSent(captchaKey: String): SmsLoginUiState {
        val snapshot = _state.value
        val next = snapshot.copy(
            phase = SmsLoginPhase.CODE_SENT,
            captchaKey = captchaKey.trim().ifBlank { snapshot.captchaKey },
            captcha = null,
            cooldownUntil = TimeMs.wallClock() + 60_000L,
            message = "滑块已完成，请输入短信验证码",
        )
        _state.value = next
        logger.i("sms slider passed accountId=${snapshot.accountId}")
        return next
    }

    fun markLoggingIn() {
        _state.value = _state.value.copy(
            phase = SmsLoginPhase.LOGGING_IN,
            message = "正在登录",
        )
    }

    fun cancelGee() {
        if (_state.value.phase == SmsLoginPhase.WAITING_GEEST) {
            _state.value = _state.value.copy(
                phase = SmsLoginPhase.IDLE,
                captcha = null,
                message = "已取消人机验证",
            )
        }
    }

    fun openHitl(message: String = "请在官方登录页完成滑块与短信验证码") {
        enterHitl(message)
    }

    fun cancelHitl() {
        if (_state.value.phase == SmsLoginPhase.HITL_WEBVIEW) {
            _state.value = _state.value.copy(
                phase = SmsLoginPhase.IDLE,
                message = "已关闭官方登录页",
            )
        }
    }

    suspend fun completeFromWebCookies(accountId: Long, cookies: List<SessionCookie>): SmsLoginUiState {
        if (!WebViewCookieParser.isLoginEstablished(cookies)) {
            return fail("尚未检测到登录 Cookie，请完成官方页滑块与验证码")
        }
        _state.value = _state.value.copy(
            phase = SmsLoginPhase.LOGGING_IN,
            message = "正在保存登录状态",
        )
        sessions.importCookies(accountId, cookies)
        return when (val session = api.checkSession(accountId)) {
            is ApiResult.Err -> fail(session.error.message)
            is ApiResult.Ok -> when (val state = session.value) {
                is SessionState.Valid -> succeed(accountId, state)
                else -> fail("登录 Cookie 已写入但会话无效")
            }
        }
    }

    suspend fun login(accountId: Long, code: String): SmsLoginUiState {
        val snapshot = _state.value
        val tel = snapshot.tel
        val captchaKey = snapshot.captchaKey
        val trimmed = code.filter { it.isDigit() }
        if (trimmed.length < 4) return fail("请输入短信验证码")
        if (tel.isBlank() || captchaKey.isNullOrBlank()) return fail("请先发送短信验证码")
        _state.value = snapshot.copy(phase = SmsLoginPhase.LOGGING_IN, message = "正在登录")
        val logged = api.loginWithSms(
            accountId = accountId,
            cid = snapshot.cid,
            tel = tel,
            code = trimmed,
            captchaKey = captchaKey,
        )
        if (logged is ApiResult.Err) {
            return when (logged.error) {
                is ApiError.VerificationRequired ->
                    enterHitl(logged.error.message.ifBlank { "登录需要人机验证，请在官方页面完成滑块" })
                else -> fail(logged.error.message.ifBlank { "登录失败" })
            }
        }
        return when (val session = api.checkSession(accountId)) {
            is ApiResult.Err -> fail(session.error.message)
            is ApiResult.Ok -> when (val state = session.value) {
                is SessionState.Valid -> succeed(accountId, state)
                else -> fail("登录 Cookie 已写入但会话无效")
            }
        }
    }

    private fun succeed(accountId: Long, state: SessionState.Valid): SmsLoginUiState {
        val next = _state.value.copy(
            phase = SmsLoginPhase.SUCCESS,
            uid = state.uid,
            uname = state.uname,
            face = state.face,
            message = "login success",
        )
        _state.value = next
        logger.i("sms login validated accountId=$accountId uid=${state.uid}")
        return next
    }

    private fun enterHitl(message: String): SmsLoginUiState {
        val snapshot = _state.value
        val accountId = snapshot.accountId
        val next = snapshot.copy(
            phase = SmsLoginPhase.HITL_WEBVIEW,
            captcha = null,
            webCookies = accountId?.let { sessions.jar(it).snapshot() } ?: snapshot.webCookies,
            message = message,
        )
        _state.value = next
        logger.i("sms login HITL webview accountId=$accountId")
        return next
    }

    private fun fail(message: String): SmsLoginUiState {
        val next = _state.value.copy(phase = SmsLoginPhase.FAILED, message = message)
        _state.value = next
        logger.w("sms login failed message=$message")
        return next
    }
}
