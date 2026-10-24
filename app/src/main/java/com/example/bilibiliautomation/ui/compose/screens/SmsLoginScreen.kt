package com.example.bilibiliautomation.ui.compose.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.example.bilibiliautomation.data.api.sms.SmsLoginPhase
import com.example.bilibiliautomation.data.api.sms.SmsLoginUiState
import com.example.bilibiliautomation.domain.model.SessionCookie
import com.example.bilibiliautomation.ui.compose.components.BiliCard
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.OutlineButton
import com.example.bilibiliautomation.ui.compose.components.PrimaryButton
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.delay

@Composable
fun SmsLoginScreen(
    state: SmsLoginUiState,
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.ACCOUNT,
    onBack: () -> Unit = {},
    onSendCode: (String) -> Unit = {},
    onSubmit: (String) -> Unit = {},
    onGeeSuccess: (String, String, String) -> Unit = { _, _, _ -> },
    onGeeError: (String) -> Unit = {},
    onGeeClosed: () -> Unit = {},
    onOpenOfficialPage: () -> Unit = {},
    onOfficialSmsSent: (String) -> Unit = {},
    onHitlSuccess: (List<SessionCookie>) -> Unit = {},
    onHitlClosed: () -> Unit = {},
    onWebViewLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var phone by remember { mutableStateOf(state.tel) }
    var code by remember { mutableStateOf("") }
    var remainSec by remember { mutableIntStateOf(0) }
    var captchaVisible by remember { mutableStateOf(false) }
    val host = remember { PassportTakeoverController() }
    LaunchedEffect(state.cooldownUntil) {
        while (true) {
            val left = ((state.cooldownUntil - TimeMs.wallClock()) / 1000L).toInt().coerceAtLeast(0)
            remainSec = left
            if (left <= 0) break
            delay(500)
        }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == SmsLoginPhase.WAITING_GEEST) captchaVisible = false
    }
    val showSlider = state.phase == SmsLoginPhase.WAITING_GEEST || state.phase == SmsLoginPhase.HITL_WEBVIEW
    val keepWebView = showSlider ||
        state.phase == SmsLoginPhase.CODE_SENT ||
        state.phase == SmsLoginPhase.LOGGING_IN
    BackHandler(enabled = showSlider) {
        if (state.phase == SmsLoginPhase.WAITING_GEEST) onGeeClosed() else onHitlClosed()
    }
    val sending = state.phase == SmsLoginPhase.LOADING_CAPTCHA ||
        state.phase == SmsLoginPhase.WAITING_GEEST ||
        state.phase == SmsLoginPhase.SENDING
    val logging = state.phase == SmsLoginPhase.LOGGING_IN
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(BiliColors.SCREEN_BG)),
        ) {
            StatusBar()
            TopHeader(title = "手机验证码登录", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = htmlDp(34))
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(htmlDp(12)))
                Text(
                    "手机号在本页填写。点获取验证码后会弹出滑块小窗（由你拖动）；通过后在本页输入短信验证码完成登录。",
                    color = Color(0xFF75859D),
                    fontSize = htmlSp(22),
                )
                Spacer(Modifier.height(htmlDp(20)))
                BiliCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = htmlDp(28), vertical = htmlDp(26))) {
                        Text("手机号", color = Color(BiliColors.NAVY), fontSize = htmlSp(24), fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.padding(top = htmlDp(14)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(htmlDp(12)),
                        ) {
                            Text(
                                state.countryLabel,
                                color = Color(0xFF5B6F8C),
                                fontSize = htmlSp(24),
                                fontWeight = FontWeight.Bold,
                            )
                            LoginField(
                                value = phone,
                                onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(15) },
                                hint = "11 位手机号",
                                keyboardType = KeyboardType.Phone,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(htmlDp(18)))
                        OutlineButton(
                            text = when {
                                sending -> "处理中…"
                                remainSec > 0 -> "${remainSec}s 后可重发"
                                else -> "获取验证码"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!sending && remainSec <= 0) onSendCode(phone)
                            },
                        )
                        Spacer(Modifier.height(htmlDp(22)))
                        Text("短信验证码", color = Color(BiliColors.NAVY), fontSize = htmlSp(24), fontWeight = FontWeight.Bold)
                        LoginField(
                            value = code,
                            onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(8) },
                            hint = "6 位验证码",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = htmlDp(14)),
                        )
                        Spacer(Modifier.height(htmlDp(22)))
                        PrimaryButton(
                            text = if (logging) "登录中…" else "登录",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !logging,
                            onClick = {
                                if (host.submitSmsCode(code)) onWebViewLogin()
                                else onSubmit(code)
                            },
                        )
                        Spacer(Modifier.height(htmlDp(14)))
                        OutlineButton(
                            text = "改用官方网页登录",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenOfficialPage,
                        )
                        if (state.message.isNotBlank() && state.phase != SmsLoginPhase.SUCCESS) {
                            Text(
                                state.message,
                                color = if (state.phase == SmsLoginPhase.FAILED) Color(0xFFFF315D) else Color(0xFF5B6F8C),
                                fontSize = htmlSp(20),
                                modifier = Modifier.padding(top = htmlDp(16)),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(htmlDp(24)))
            }
            BottomNav(selected = selectedTab, onSelect = onTabSelect)
        }
        if (keepWebView) {
            Box(
                modifier = if (showSlider) {
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000))
                } else {
                    Modifier
                        .size(htmlDp(1))
                        .align(Alignment.BottomEnd)
                },
                contentAlignment = Alignment.Center,
            ) {
                BiliCard(
                    modifier = if (showSlider) {
                        Modifier
                            .padding(horizontal = htmlDp(28))
                            .fillMaxWidth()
                            .height(htmlDp(620))
                    } else {
                        Modifier.fillMaxSize()
                    },
                ) {
                    Column(Modifier.fillMaxSize()) {
                        if (showSlider) {
                            TopHeader(
                                title = "请完成滑块",
                                onBack = if (state.phase == SmsLoginPhase.WAITING_GEEST) onGeeClosed else onHitlClosed,
                            )
                            Text(
                                if (captchaVisible) "请拖动滑块，通过后返回填写验证码"
                                else "正在打开滑块，请稍候…",
                                color = Color(0xFF75859D),
                                fontSize = htmlSp(20),
                                modifier = Modifier.padding(horizontal = htmlDp(24), vertical = htmlDp(8)),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(htmlDp(16))),
                        ) {
                            GeeTestWebView(
                                phone = state.tel.ifBlank { phone },
                                cookies = state.webCookies,
                                controller = host,
                                onCaptchaVisible = { captchaVisible = true },
                                onOfficialSmsSent = onOfficialSmsSent,
                                onLoginCookies = onHitlSuccess,
                                onError = onGeeError,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(htmlDp(78))
            .background(Color(0xFFF4F7FB), RoundedCornerShape(htmlDp(13)))
            .padding(horizontal = htmlDp(20)),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(hint, color = Color(0xFF9AA8B8), fontSize = htmlSp(24))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color(BiliColors.NAVY),
                fontSize = htmlSp(26),
                fontWeight = FontWeight.Bold,
            ),
            cursorBrush = SolidColor(Color(BiliColors.PINK)),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "手机验证码登录", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun SmsLoginScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.ACCOUNT) }
    BiliTheme {
        SmsLoginScreen(
            state = SmsLoginUiState(message = "验证码已发送"),
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
        )
    }
}
