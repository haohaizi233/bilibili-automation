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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

data class TaskSettingsUiState(
    val interval: String = "5",
    val retries: String = "3",
    val cooldown: String = "30",
    val dailyLimit: String = "200",
    val parallelTasks: Boolean = true,
    val smartStatus: Boolean = true,
    val autoSwitchAccount: Boolean = true,
    val autoRetry: Boolean = true,
    val notifyOnComplete: Boolean = true,
    val verboseLog: Boolean = false,
)

@Composable
fun TaskSettingsScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.TASK,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    state: TaskSettingsUiState = TaskSettingsUiState(),
    onStateChange: (TaskSettingsUiState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(title = "任务设置", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(46))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(htmlDp(20)),
        ) {
            Spacer(Modifier.height(htmlDp(8)))
            BasicSettingsCard(state, onStateChange)
            AdvancedSettingsCard(state, onStateChange)
            PrimaryButton(text = "保存设置", modifier = Modifier.fillMaxWidth(), onClick = onSave)
            Spacer(Modifier.height(htmlDp(8)))
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun BasicSettingsCard(
    state: TaskSettingsUiState,
    onStateChange: (TaskSettingsUiState) -> Unit,
) {
    BiliCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(htmlDp(26), htmlDp(30), htmlDp(30), htmlDp(20))) {
            Text("基础设置", color = Color(BiliColors.NAVY), fontSize = htmlSp(29), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(htmlDp(27)))
            SettingInputRow("操作间隔", state.interval, "秒") { onStateChange(state.copy(interval = it)) }
            SettingInputRow("错误重试", state.retries, "次") { onStateChange(state.copy(retries = it)) }
            SettingInputRow("账号冷却时间", state.cooldown, "分钟") { onStateChange(state.copy(cooldown = it)) }
            SettingInputRow("单账号每日上限", state.dailyLimit, "") { onStateChange(state.copy(dailyLimit = it)) }
            SettingToggleRow("同时运行任务", state.parallelTasks) { onStateChange(state.copy(parallelTasks = it)) }
        }
    }
}

@Composable
private fun AdvancedSettingsCard(
    state: TaskSettingsUiState,
    onStateChange: (TaskSettingsUiState) -> Unit,
) {
    BiliCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(htmlDp(25), htmlDp(30), htmlDp(30), htmlDp(20))) {
            Text("高级设置", color = Color(BiliColors.NAVY), fontSize = htmlSp(29), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(htmlDp(27)))
            SettingToggleRow("智能识别状态 (推荐)", state.smartStatus) { onStateChange(state.copy(smartStatus = it)) }
            SettingToggleRow("自动切换账号", state.autoSwitchAccount) { onStateChange(state.copy(autoSwitchAccount = it)) }
            SettingToggleRow("失败自动重试", state.autoRetry) { onStateChange(state.copy(autoRetry = it)) }
            SettingToggleRow("任务完成通知", state.notifyOnComplete) { onStateChange(state.copy(notifyOnComplete = it)) }
            SettingToggleRow("详细日志记录", state.verboseLog, last = true) { onStateChange(state.copy(verboseLog = it)) }
        }
    }
}

@Composable
private fun SettingInputRow(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(htmlDp(82)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(htmlDp(10)),
    ) {
        Text(label, color = Color(0xFF1C2E4C), fontSize = htmlSp(26), modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(htmlDp(176))
                .height(htmlDp(65))
                .background(Color(0xFFF3F6F9), RoundedCornerShape(htmlDp(12)))
                .padding(horizontal = htmlDp(22)),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(Color(BiliColors.PINK)),
                textStyle = TextStyle(
                    color = Color(BiliColors.NAVY),
                    fontSize = htmlSp(25),
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Text(unit, color = Color(0xFF1F3150), fontSize = htmlSp(24), modifier = Modifier.width(htmlDp(60)))
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    last: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(htmlDp(82)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Color(0xFF1C2E4C), fontSize = htmlSp(26), modifier = Modifier.weight(1f))
            BiliSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (!last) {
            Box(Modifier.fillMaxWidth().height(htmlDp(1)).background(Color(0xFFEDF1F5)))
        }
    }
}

@Preview(name = "07 任务设置", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun TaskSettingsScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.TASK) }
    var state by remember { mutableStateOf(TaskSettingsUiState()) }
    BiliTheme {
        TaskSettingsScreen(
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            onBack = {},
            onSave = {},
            state = state,
            onStateChange = { state = it },
        )
    }
}
