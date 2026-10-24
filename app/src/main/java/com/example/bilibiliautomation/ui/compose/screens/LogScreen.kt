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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
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
import com.example.bilibiliautomation.ui.compose.components.EmptyArt
import com.example.bilibiliautomation.ui.compose.components.EmptyHint
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

enum class LogLevel(val tag: String, val color: Long, val bg: Long) {
    INFO("[INFO]", BiliColors.BLUE, BiliColors.FILTER_INFO_BG),
    SUCCESS("[SUCCESS]", BiliColors.GREEN, BiliColors.FILTER_SUCCESS_BG),
    WARN("[WARN]", BiliColors.ORANGE, BiliColors.FILTER_WARN_BG),
    ERROR("[ERROR]", BiliColors.RED, BiliColors.FILTER_ERROR_BG),
}

data class LogEntry(val time: String, val level: LogLevel, val message: String, val sub: String? = null, val errorHighlight: Boolean = false)

@Composable
fun LogScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.TASK,
    onBack: () -> Unit = {},
    onClear: () -> Unit = {},
    logs: List<LogEntry>? = null,
    selectedFilter: Int = 0,
    onFilterSelect: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val rows = logs ?: listOf(
        LogEntry("14:32:15", LogLevel.INFO, "开始遍历视频", "BV1xx411c7mD"),
        LogEntry("14:32:18", LogLevel.SUCCESS, "关注用户", "UID: 67283910"),
        LogEntry("14:32:20", LogLevel.INFO, "已跳过（已关注）", "UID: 12345678"),
        LogEntry("14:32:25", LogLevel.WARN, "操作过于频繁", "等待 30 秒后继续..."),
        LogEntry("14:32:55", LogLevel.INFO, "切换账号：账号2"),
        LogEntry("14:33:10", LogLevel.ERROR, "账号登录失效", "请检查网络或Cookie", errorHighlight = true),
        LogEntry("14:33:12", LogLevel.INFO, "开始登录新账号"),
        LogEntry("14:34:01", LogLevel.SUCCESS, "登录成功", "UID: 517327498"),
        LogEntry("14:34:05", LogLevel.INFO, "继续执行任务..."),
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(
            title = "任务日志",
            onBack = onBack,
            trailing = {
                Row(
                    modifier = Modifier.clickableNoRipple { onClear() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = Color(0xFF73839A), modifier = Modifier.size(htmlDp(28)))
                    Text("清空", color = Color(0xFF73839A), fontSize = htmlSp(25), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = htmlDp(9)))
                }
            },
        )
        FilterRow(selected = selectedFilter, onSelect = onFilterSelect)
        if (rows.isEmpty()) {
            EmptyHint(
                art = EmptyArt.LOG,
                title = "还没有任务日志",
                subtitle = "开始任务后，浏览视频、关注和跳过都会记在这里，方便你随时回看。",
                modifier = Modifier.weight(1f),
            )
        } else {
            BiliCard(modifier = Modifier.fillMaxWidth().padding(horizontal = htmlDp(38), vertical = htmlDp(8))) {
                LazyColumn(modifier = Modifier.padding(horizontal = htmlDp(16), vertical = htmlDp(10))) {
                    items(rows) { entry ->
                        LogRow(entry)
                        if (rows.indexOf(entry) < rows.lastIndex) {
                            Box(Modifier.fillMaxWidth().height(htmlDp(1)).background(Color(0xFFE3E9EF)))
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun FilterRow(selected: Int, onSelect: (Int) -> Unit) {
    val filters = listOf("全部" to BiliColors.PINK, "信息" to BiliColors.FILTER_INFO_BG, "成功" to BiliColors.FILTER_SUCCESS_BG, "警告" to BiliColors.FILTER_WARN_BG, "错误" to BiliColors.FILTER_ERROR_BG)
    val fgColors = listOf(BiliColors.PINK, BiliColors.BLUE, BiliColors.GREEN, BiliColors.ORANGE, BiliColors.RED)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(htmlDp(91))
            .padding(horizontal = htmlDp(39)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(htmlDp(14)),
    ) {
        filters.forEachIndexed { index, (label, bg) ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .height(htmlDp(56))
                    .background(
                        if (isSelected) Color(BiliColors.PINK) else Color(bg),
                        RoundedCornerShape(htmlDp(11)),
                    )
                    .clickableNoRipple { onSelect(index) }
                    .padding(horizontal = htmlDp(24)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else Color(fgColors[index]),
                    fontSize = htmlSp(23),
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = htmlDp(24), horizontal = htmlDp(12)),
    ) {
        Box(
            modifier = Modifier
                .padding(top = htmlDp(6))
                .size(htmlDp(21))
                .background(Color(entry.level.color), CircleShape),
        )
        Text(
            entry.time,
            color = Color(0xFF71829A),
            fontSize = htmlSp(23),
            modifier = Modifier
                .padding(start = htmlDp(12))
                .width(htmlDp(134)),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color(entry.level.color), fontWeight = FontWeight.Black)) {
                    append(entry.level.tag)
                }
                append(" ")
                if (entry.errorHighlight) {
                    withStyle(SpanStyle(color = Color(0xFFFF1747), fontWeight = FontWeight.Black)) {
                        append(entry.message)
                    }
                } else {
                    append(entry.message)
                }
                entry.sub?.let {
                    append("\n")
                    withStyle(SpanStyle(color = Color(0xFF6F819A))) {
                        append(it)
                    }
                }
            },
            color = Color(0xFF1F3150),
            fontSize = htmlSp(23),
            modifier = Modifier
                .padding(start = htmlDp(12))
                .weight(1f),
        )
    }
}

@Preview(name = "09 任务日志", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun LogScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.TASK) }
    BiliTheme {
        LogScreen(selectedTab = selectedTab, onTabSelect = { selectedTab = it }, onBack = {}, onClear = {})
    }
}

@Preview(name = "09 任务日志 · 空", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun LogScreenEmptyPreview() {
    BiliTheme {
        LogScreen(onTabSelect = {}, logs = emptyList())
    }
}
