package com.example.bilibiliautomation.ui.compose.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bilibiliautomation.ui.compose.components.BiliCard
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.PrimaryButton
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

@Composable
fun SettingsScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.SETTINGS,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onTaskSettings: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    versionName: String = "v1.0.0",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(title = "设置", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(34))
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(htmlDp(8)))
            SettingsGroup(title = "数据管理") {
                SettingsRow(icon = Icons.Filled.Info, label = "清空历史记录", last = true, onClick = onClearHistory)
            }
            Spacer(Modifier.height(htmlDp(34)))
            SettingsGroup(title = "应用设置") {
                SettingsRow(icon = Icons.Filled.Notifications, label = "通知设置", onClick = onTaskSettings)
                SettingsRow(icon = Icons.Filled.Info, label = "版本号", value = versionName, last = true)
            }
            Spacer(Modifier.height(htmlDp(37)))
            PrimaryButton(text = "退出登录", modifier = Modifier.fillMaxWidth(), onClick = onLogout)
            Spacer(Modifier.height(htmlDp(18)))
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    BiliCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(htmlDp(86))
                    .background(Color.White)
                    .padding(horizontal = htmlDp(31)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(title, color = Color(BiliColors.NAVY), fontSize = htmlSp(29), fontWeight = FontWeight.Black)
            }
            Box(Modifier.fillMaxWidth().height(htmlDp(1)).background(Color(0xFFEDF1F5)))
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String? = null,
    last: Boolean = false,
    onClick: () -> Unit = {},
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(htmlDp(96))
                .clickableNoRipple { onClick() }
                .padding(start = htmlDp(31), end = htmlDp(28)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(htmlDp(34))
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF5B6F8C), modifier = Modifier.size(htmlDp(28)))
            }
            Text(label, color = Color(0xFF52657F), fontSize = htmlSp(27), modifier = Modifier.padding(start = htmlDp(24)).weight(1f))
            value?.let {
                Text(it, color = Color(0xFF76869C), fontSize = htmlSp(24), modifier = Modifier.padding(end = htmlDp(14)))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF7B8A9F), modifier = Modifier.size(htmlDp(20)))
        }
        if (!last) {
            Box(Modifier.fillMaxWidth().height(htmlDp(1)).background(Color(0xFFDFE6ED)))
        }
    }
}

@Preview(name = "10 设置页面", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun SettingsScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.SETTINGS) }
    BiliTheme {
        SettingsScreen(selectedTab = selectedTab, onTabSelect = { selectedTab = it }, onBack = {}, onLogout = {})
    }
}
