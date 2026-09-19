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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bilibiliautomation.ui.compose.components.Avatar
import com.example.bilibiliautomation.ui.compose.components.BiliCard
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.DotsMenu
import com.example.bilibiliautomation.ui.compose.components.MetricCell
import com.example.bilibiliautomation.ui.compose.components.PrimaryButton
import com.example.bilibiliautomation.ui.compose.components.ProgressTrack
import com.example.bilibiliautomation.ui.compose.components.SectionTitle
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.Tag
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.preview.PreviewMockData
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

enum class HomeTaskStatus { RUNNING, PAUSED, IDLE }

@Composable
fun HomeScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.HOME,
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onStart: () -> Unit = {},
    onTaskSettings: () -> Unit = {},
    onAccountMore: () -> Unit = {},
    taskStatus: HomeTaskStatus = HomeTaskStatus.RUNNING,
    elapsed: String = PreviewMockData.HOME_ELAPSED,
    accountPainter: Painter? = null,
    upPainter: Painter? = null,
    brandPainter: Painter? = null,
    accountSubtitle: String = PreviewMockData.HOME_ACCOUNT_SUB,
    upSubtitle: String = PreviewMockData.HOME_UP_SUB,
    progressDone: Int = PreviewMockData.HOME_PROGRESS_DONE,
    progressTotal: Int = PreviewMockData.HOME_PROGRESS_TOTAL,
    followed: Int = PreviewMockData.HOME_FOLLOWED,
    skipped: Int = PreviewMockData.HOME_SKIPPED,
    failed: Int = PreviewMockData.HOME_FAILED,
    accountImageUrl: String? = null,
    upImageUrl: String? = null,
    accountStatus: AccountStatus = AccountStatus.OK,
    upStatus: AccountStatus = AccountStatus.OK,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(27))
                .verticalScroll(rememberScrollState()),
        ) {
            BrandHeader(brandPainter)
            Spacer(Modifier.height(htmlDp(4)))
            StatusCard(
                status = taskStatus,
                elapsed = elapsed,
                onPause = onPause,
                onStart = onStart,
            )
            CurrentAccountRow(painter = accountPainter, imageUrl = accountImageUrl, subtitle = accountSubtitle, status = accountStatus, onMore = onAccountMore)
            CurrentUpRow(painter = upPainter, imageUrl = upImageUrl, subtitle = upSubtitle, status = upStatus, onMore = onTaskSettings)
            ProgressCard(
                done = progressDone,
                total = progressTotal.coerceAtLeast(1),
                percent = if (progressTotal <= 0) 0 else (progressDone * 100 / progressTotal),
            )
            TodayDataCard(followed = followed, skipped = skipped, failed = failed)
            Spacer(Modifier.height(htmlDp(17)))
            PrimaryButton(
                text = if (taskStatus == HomeTaskStatus.IDLE) "开始任务" else "停止任务",
                modifier = Modifier.fillMaxWidth(),
                onClick = if (taskStatus == HomeTaskStatus.IDLE) onStart else onStop,
            )
            Spacer(Modifier.height(htmlDp(18)))
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun BrandHeader(brandPainter: Painter?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = htmlDp(8), bottom = htmlDp(24)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (brandPainter != null) {
            Image(
                painter = brandPainter,
                contentDescription = "Bilibili",
                modifier = Modifier.size(htmlDp(80)),
            )
        } else {
            Box(
                Modifier
                    .size(htmlDp(80))
                    .background(
                        Brush.linearGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                        RoundedCornerShape(htmlDp(20)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("B", color = Color.White, fontSize = htmlSp(44), fontWeight = FontWeight.Black)
            }
        }
        Column(Modifier.padding(start = htmlDp(18)).weight(1f).widthIn(min = 0.dp)) {
            Text(
                "Bilibili 自动化助手",
                color = Color(BiliColors.NAVY),
                fontSize = htmlSp(30),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "让喜欢的UP被更多人看到",
                color = Color(0xFF75839A),
                fontSize = htmlSp(22),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = htmlDp(9)),
            )
        }
    }
}

@Composable
private fun StatusCard(
    status: HomeTaskStatus,
    elapsed: String,
    onPause: () -> Unit,
    onStart: () -> Unit,
) {
    val title = when (status) {
        HomeTaskStatus.RUNNING -> "任务运行中"
        HomeTaskStatus.PAUSED -> "任务已暂停"
        HomeTaskStatus.IDLE -> "当前无任务"
    }
    val action = when (status) {
        HomeTaskStatus.RUNNING -> "暂停"
        HomeTaskStatus.PAUSED -> "继续"
        HomeTaskStatus.IDLE -> "开始"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(htmlDp(172))
            .background(
                Brush.linearGradient(
                    listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2), Color(0xFFFF6A9A)),
                ),
                RoundedCornerShape(htmlDp(18)),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = htmlDp(28), vertical = htmlDp(26)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, color = Color.White, fontSize = htmlSp(29), fontWeight = FontWeight.Black)
                Text(
                    if (status == HomeTaskStatus.IDLE) "00:00:00" else elapsed,
                    color = Color.White,
                    fontSize = htmlSp(52),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = htmlDp(12)),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickableNoRipple {
                    if (status == HomeTaskStatus.IDLE) onStart() else onPause()
                },
            ) {
                Box(
                    Modifier
                        .size(htmlDp(82))
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (status == HomeTaskStatus.RUNNING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = action,
                        tint = Color(BiliColors.PINK),
                        modifier = Modifier.size(htmlDp(34)),
                    )
                }
                Text(action, color = Color.White, fontSize = htmlSp(22), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = htmlDp(7)))
            }
        }
    }
}

@Composable
private fun CurrentAccountRow(painter: Painter?, imageUrl: String?, subtitle: String, status: AccountStatus, onMore: () -> Unit = {}) {
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(18))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = htmlDp(111))
                .padding(horizontal = htmlDp(20), vertical = htmlDp(15)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(fallbackText = subtitle.take(1).ifBlank { "账" }, size = htmlDp(70), painter = painter, imageUrl = imageUrl)
            Column(Modifier.padding(start = htmlDp(18)).weight(1f).widthIn(min = 0.dp)) {
                Text("当前账号", color = Color(BiliColors.NAVY), fontSize = htmlSp(26), fontWeight = FontWeight.Black, maxLines = 1)
                Text(
                    subtitle,
                    color = Color(0xFF74849C),
                    fontSize = htmlSp(18),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = htmlDp(7)),
                )
            }
            Tag(status.label, status.style)
            DotsMenu(Modifier.padding(start = htmlDp(11)), onClick = onMore)
        }
    }
}

@Composable
private fun CurrentUpRow(painter: Painter?, imageUrl: String?, subtitle: String, status: AccountStatus, onMore: () -> Unit) {
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(18))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = htmlDp(111))
                .padding(horizontal = htmlDp(20), vertical = htmlDp(15)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(fallbackText = subtitle.take(1).ifBlank { "UP" }, size = htmlDp(70), painter = painter, imageUrl = imageUrl)
            Column(Modifier.padding(start = htmlDp(18)).weight(1f).widthIn(min = 0.dp)) {
                Text("当前UP", color = Color(BiliColors.NAVY), fontSize = htmlSp(26), fontWeight = FontWeight.Black, maxLines = 1)
                Text(
                    subtitle,
                    color = Color(0xFF74849C),
                    fontSize = htmlSp(18),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = htmlDp(7)),
                )
            }
            Tag(status.label, status.style)
            DotsMenu(Modifier.padding(start = htmlDp(11)), onClick = onMore)
        }
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int, percent: Int) {
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(18))) {
        Column(Modifier.padding(horizontal = htmlDp(20), vertical = htmlDp(21))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("当前进度")
                Text("$done / $total", color = Color(0xFF708099), fontSize = htmlSp(23), fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(14)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(htmlDp(18)),
            ) {
                ProgressTrack(fraction = done.toFloat() / total, modifier = Modifier.weight(1f))
                Text("$percent%", color = Color(0xFF75849A), fontSize = htmlSp(21))
            }
        }
    }
}

@Composable
private fun TodayDataCard(followed: Int, skipped: Int, failed: Int) {
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(18))) {
        Column(Modifier.padding(htmlDp(20))) {
            Text("今日数据", color = Color(BiliColors.NAVY), fontSize = htmlSp(26), fontWeight = FontWeight.Black)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(15)),
                horizontalArrangement = Arrangement.spacedBy(htmlDp(12)),
            ) {
                MetricCell("已关注", followed.toString(), Color(BiliColors.METRIC_BLUE), Modifier.weight(1f))
                MetricCell("已跳过", skipped.toString(), Color(BiliColors.METRIC_NEUTRAL), Modifier.weight(1f))
                MetricCell("失败", failed.toString(), Color(BiliColors.METRIC_PINK), Modifier.weight(1f))
            }
        }
    }
}

@Preview(name = "01 首页 - 控制面板", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun HomeScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.HOME) }
    BiliTheme {
        HomeScreen(
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            onPause = {},
            onStop = {},
        )
    }
}
