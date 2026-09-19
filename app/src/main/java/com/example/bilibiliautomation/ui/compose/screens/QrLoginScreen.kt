package com.example.bilibiliautomation.ui.compose.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bilibiliautomation.ui.compose.components.BiliCard
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

@Composable
fun QrLoginScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.ACCOUNT,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    validityText: String = "二维码有效期 5 分钟\n请勿向他人展示二维码",
    qrBitmap: ImageBitmap? = null,
    currentStep: Int = 0,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(title = "添加账号", onBack = onBack)
        StepIndicator(current = currentStep)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(28), vertical = htmlDp(18)),
        ) {
            BiliCard(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = htmlDp(51)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    QrCodeBox(bitmap = qrBitmap, loading = loading)
                    Text(
                        buildAnnotatedString {
                            append("使用 ")
                            withStyle(SpanStyle(color = Color(BiliColors.PINK), fontWeight = FontWeight.Black)) {
                                append("哔哩哔哩 APP")
                            }
                            append(" 扫码登录")
                        },
                        color = Color(BiliColors.NAVY),
                        fontSize = htmlSp(28),
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = htmlDp(31)),
                    )
                    Text(
                        validityText,
                        color = Color(0xFF75859D),
                        fontSize = htmlSp(25),
                        modifier = Modifier.padding(top = htmlDp(30)),
                    )
                    Row(
                        modifier = Modifier
                            .padding(top = htmlDp(44))
                            .width(htmlDp(390))
                            .height(htmlDp(78))
                            .background(Color.White, RoundedCornerShape(htmlDp(13)))
                            .shadow(htmlDp(0), RoundedCornerShape(htmlDp(13)))
                            .clickableNoRipple { onRefresh() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Color(BiliColors.PINK),
                            modifier = Modifier.size(htmlDp(31)),
                        )
                        Text("刷新二维码", color = Color(BiliColors.PINK), fontSize = htmlSp(27), fontWeight = FontWeight.Black, modifier = Modifier.padding(start = htmlDp(15)))
                    }
                }
            }
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun StepIndicator(current: Int) {
    val steps = listOf("扫码登录", "确认登录", "保存账号")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(htmlDp(92))
            .padding(horizontal = htmlDp(38)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(htmlDp(8)),
    ) {
        steps.forEachIndexed { index, label ->
            val active = index <= current
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(htmlDp(40))
                        .background(
                            if (active) Color(BiliColors.BLUE) else Color(0xFFEDF2F7),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        color = if (active) Color.White else Color(0xFF8C9BB0),
                        fontSize = htmlSp(20),
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    label,
                    color = if (active) Color(0xFF17233C) else Color(0xFF9AA6B8),
                    fontSize = htmlSp(22),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = htmlDp(10)),
                )
            }
            if (index < steps.lastIndex) {
                Box(
                    Modifier
                        .width(htmlDp(20))
                        .height(htmlDp(3))
                        .background(Color(0xFFDCE3EB), RoundedCornerShape(htmlDp(2))),
                )
            }
        }
    }
}

@Composable
private fun QrCodeBox(
    bitmap: ImageBitmap?,
    loading: Boolean,
) {
    Box(
        modifier = Modifier
            .size(width = htmlDp(291), height = htmlDp(283))
            .background(Color.White, RoundedCornerShape(htmlDp(12)))
            .semantics { contentDescription = "二维码" },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "登录二维码",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(htmlDp(12)),
            )
        } else {
            QrPlaceholderPattern()
            if (loading) {
                Text(
                    "正在获取登录二维码",
                    color = Color(BiliColors.NAVY),
                    fontSize = htmlSp(20),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xE6FFFFFF), RoundedCornerShape(htmlDp(8)))
                        .padding(horizontal = htmlDp(12), vertical = htmlDp(8)),
                )
            }
        }
    }
}

@Composable
private fun QrPlaceholderPattern() {
    Column {
        repeat(7) { row ->
            Row {
                repeat(7) { col ->
                    val on = (row * 7 + col) % 3 == 0 || row == 0 || row == 6 || col == 0 || col == 6
                    Box(
                        Modifier
                            .size(htmlDp(28))
                            .padding(htmlDp(2))
                            .background(if (on) Color(BiliColors.NAVY) else Color.White),
                    )
                }
            }
        }
    }
}

@Preview(name = "03 扫码登录", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun QrLoginScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.ACCOUNT) }
    BiliTheme {
        QrLoginScreen(selectedTab = selectedTab, onTabSelect = { selectedTab = it }, onBack = {}, onRefresh = {})
    }
}
