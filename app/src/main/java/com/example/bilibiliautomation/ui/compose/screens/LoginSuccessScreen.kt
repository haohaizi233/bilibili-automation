package com.example.bilibiliautomation.ui.compose.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bilibiliautomation.ui.compose.components.Avatar
import com.example.bilibiliautomation.ui.compose.components.BiliCard
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.PrimaryButton
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.preview.PreviewMockData
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

@Composable
fun LoginSuccessScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.ACCOUNT,
    onBack: () -> Unit = {},
    onDone: () -> Unit = {},
    onSetCurrent: () -> Unit = {},
    avatarPainter: Painter? = null,
    avatarUrl: String? = null,
    userName: String = "动漫小助手",
    uid: String = "67283910",
    fans: String = PreviewMockData.LOGIN_SUCCESS_FANS,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(title = "登录成功", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(31))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SuccessBadge()
            Text("账号添加成功!", color = Color(BiliColors.NAVY), fontSize = htmlSp(39), fontWeight = FontWeight.Black)
            Avatar(
                fallbackText = userName.take(1).ifBlank { "账" },
                size = htmlDp(141),
                painter = avatarPainter,
                imageUrl = avatarUrl,
                modifier = Modifier.padding(top = htmlDp(29)),
            )
            InfoCard(userName = userName, uid = uid, fans = fans)
            CheckRow(onSetCurrent = onSetCurrent)
            Spacer(Modifier.height(htmlDp(18)))
            PrimaryButton(text = "完成", modifier = Modifier.fillMaxWidth(), onClick = onDone)
            Spacer(Modifier.height(htmlDp(18)))
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun SuccessBadge() {
    Box(
        modifier = Modifier
            .size(htmlDp(164))
            .padding(top = htmlDp(38)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(htmlDp(164))
                .background(Color(0x3845DA9E), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(htmlDp(126))
                .shadow(htmlDp(10), CircleShape, ambientColor = Color(0x331FBE71))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF58DCA6), Color(0xFF10BD6B))),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(htmlDp(64)),
            )
        }
    }
}

@Composable
private fun InfoCard(userName: String, uid: String, fans: String) {
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(18))) {
        Column(Modifier.padding(htmlDp(24))) {
            Text("账号信息", color = Color(BiliColors.NAVY), fontSize = htmlSp(28), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(htmlDp(15)))
            InfoRow("用户名", userName)
            InfoRow("UID", uid)
            InfoRow("粉丝", fans)
            InfoRow("账号状态", "正常", valueColor = Color(BiliColors.GREEN), last = true)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = Color(0xFF111B2D), last: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(htmlDp(58)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color(0xFF71819B), fontSize = htmlSp(24))
        Text(value, color = valueColor, fontSize = htmlSp(24), fontWeight = if (valueColor == Color(BiliColors.GREEN)) FontWeight.Black else FontWeight.Bold)
    }
    if (!last) {
        Box(Modifier.fillMaxWidth().height(htmlDp(1)).background(Color(0xFFDCE4EC)))
    }
}

@Composable
private fun CheckRow(onSetCurrent: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = htmlDp(14), start = htmlDp(12))
            .clickableNoRipple { onSetCurrent() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(htmlDp(35))
                .background(Color(BiliColors.BLUE), RoundedCornerShape(htmlDp(6))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(htmlDp(22)))
        }
        Text(
            "立即设置为可用账号",
            color = Color(0xFF51647F),
            fontSize = htmlSp(24),
            modifier = Modifier.padding(start = htmlDp(14)),
        )
    }
}

@Preview(name = "04 账号登录成功", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun LoginSuccessScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.ACCOUNT) }
    BiliTheme {
        LoginSuccessScreen(selectedTab = selectedTab, onTabSelect = { selectedTab = it }, onBack = {}, onDone = {})
    }
}
