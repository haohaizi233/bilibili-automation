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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.bilibiliautomation.ui.compose.components.BiliSwitch
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
    onNotifications: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    unattendedMode: Boolean = false,
    onUnattendedChange: (Boolean) -> Unit = {},
    versionName: String = "v1.0.0",
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }
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
                .padding(horizontal = htmlDp(34)),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(htmlDp(8)))
                SettingsGroup(title = "数据管理") {
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        label = "清空历史记录",
                        last = true,
                        onClick = { confirmClear = true },
                    )
                }
                Spacer(Modifier.height(htmlDp(34)))
            SettingsGroup(title = "应用设置") {
                SettingsRow(icon = Icons.Filled.Tune, label = "任务设置", onClick = onTaskSettings)
                SettingsRow(icon = Icons.Filled.Notifications, label = "通知设置", onClick = onNotifications)
                SettingsToggleRow(
                    icon = Icons.Filled.Bolt,
                    label = "无人值守自动换号",
                    subtitle = "号异常后轮询号池，没有可用号就休息等待。开启时向系统申请通知和忽略电池优化；每次进入复查，缺权限自动关闭。",
                    checked = unattendedMode,
                    onCheckedChange = onUnattendedChange,
                )
                SettingsRow(icon = Icons.Filled.Info, label = "版本号", value = versionName, last = true)
            }
                Spacer(Modifier.height(htmlDp(37)))
                PrimaryButton(text = "退出登录", modifier = Modifier.fillMaxWidth(), onClick = onLogout)
                Spacer(Modifier.height(htmlDp(18)))
            }
            ContactFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = htmlDp(18)),
            )
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空历史记录") },
            text = {
                Text("将清除任务进度、操作记录、视频/评论缓存和日志。已登录账号与目标 UP 不会删除。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearHistory()
                    },
                ) {
                    Text("清空", color = Color(BiliColors.PINK), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("取消", color = Color(0xFF5B6F8C))
                }
            },
        )
    }
}

@Composable
private fun ContactFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = htmlDp(8)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("联系方式", color = Color(0xFF9AA8B8), fontSize = htmlSp(20), fontWeight = FontWeight.Bold)
        Text("电话：15603838733", color = Color(0xFF7B8A9F), fontSize = htmlSp(20), modifier = Modifier.padding(top = htmlDp(8)))
        Text("邮箱：haohaizi554@gmail.com", color = Color(0xFF7B8A9F), fontSize = htmlSp(20), modifier = Modifier.padding(top = htmlDp(4)))
        Text("微信：MemoryLeak2023", color = Color(0xFF7B8A9F), fontSize = htmlSp(20), modifier = Modifier.padding(top = htmlDp(4)))
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

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = htmlDp(31), end = htmlDp(28), top = htmlDp(22), bottom = htmlDp(22)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(htmlDp(34)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF5B6F8C), modifier = Modifier.size(htmlDp(28)))
            }
            Column(Modifier.padding(start = htmlDp(24)).weight(1f).padding(end = htmlDp(16))) {
                Text(label, color = Color(0xFF52657F), fontSize = htmlSp(27))
                Text(
                    subtitle,
                    color = Color(0xFF9AA8B8),
                    fontSize = htmlSp(18),
                    modifier = Modifier.padding(top = htmlDp(6)),
                )
            }
            BiliSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
        Box(Modifier.fillMaxWidth().height(htmlDp(1)).background(Color(0xFFDFE6ED)))
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
