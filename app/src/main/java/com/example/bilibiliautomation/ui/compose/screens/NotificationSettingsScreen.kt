package com.example.bilibiliautomation.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.example.bilibiliautomation.ui.compose.components.BiliCard
import com.example.bilibiliautomation.ui.compose.components.BiliSwitch
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

@Composable
fun NotificationSettingsScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.SETTINGS,
    onBack: () -> Unit = {},
    notifyOnComplete: Boolean = true,
    onNotifyOnCompleteChange: (Boolean) -> Unit = {},
    onOpenSystemSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(title = "通知设置", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(34))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(htmlDp(20)),
        ) {
            Spacer(Modifier.height(htmlDp(8)))
            BiliCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(htmlDp(26), htmlDp(24), htmlDp(30), htmlDp(12))) {
                    Text("应用通知", color = Color(BiliColors.NAVY), fontSize = htmlSp(29), fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(htmlDp(12)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(htmlDp(82)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = htmlDp(16))) {
                            Text("任务状态通知", color = Color(0xFF1C2E4C), fontSize = htmlSp(26))
                            Text(
                                "任务完成、失败或暂停时提醒",
                                color = Color(0xFF7B8A9F),
                                fontSize = htmlSp(18),
                                modifier = Modifier.padding(top = htmlDp(4)),
                            )
                        }
                        BiliSwitch(checked = notifyOnComplete, onCheckedChange = onNotifyOnCompleteChange)
                    }
                }
            }
            BiliCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(htmlDp(96))
                        .clickableNoRipple { onOpenSystemSettings() }
                        .padding(horizontal = htmlDp(26)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("系统通知权限", color = Color(0xFF1C2E4C), fontSize = htmlSp(26), fontWeight = FontWeight.Bold)
                        Text(
                            "前往系统设置允许本应用通知",
                            color = Color(0xFF7B8A9F),
                            fontSize = htmlSp(18),
                            modifier = Modifier.padding(top = htmlDp(4)),
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF7B8A9F))
                }
            }
            Text(
                "任务运行中的前台通知由系统要求保留，无法关闭。",
                color = Color(0xFF9AA8B8),
                fontSize = htmlSp(18),
                modifier = Modifier.padding(horizontal = htmlDp(8), vertical = htmlDp(4)),
            )
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Preview(name = "通知设置", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun NotificationSettingsScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.SETTINGS) }
    var notify by remember { mutableStateOf(true) }
    BiliTheme {
        NotificationSettingsScreen(
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            notifyOnComplete = notify,
            onNotifyOnCompleteChange = { notify = it },
        )
    }
}
