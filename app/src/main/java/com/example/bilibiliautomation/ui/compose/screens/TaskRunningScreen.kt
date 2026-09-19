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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bilibiliautomation.ui.compose.components.Avatar
import com.example.bilibiliautomation.ui.compose.components.BiliCard
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.DotsMenu
import com.example.bilibiliautomation.ui.compose.components.MetricCell
import com.example.bilibiliautomation.ui.compose.components.PrimaryButton
import com.example.bilibiliautomation.ui.compose.components.ProgressTrack
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.Tag
import com.example.bilibiliautomation.ui.compose.components.TagStyle
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.preview.PreviewMockData
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp
import com.example.bilibiliautomation.util.MediaUrls

enum class TaskRunningPhase { IDLE, RUNNING, PAUSED, FINISHED, FAILED, CANCELLED }

data class TaskRunningUiState(
    val phase: TaskRunningPhase = TaskRunningPhase.IDLE,
    val upName: String,
    val upUid: String,
    val upAvatarUrl: String = "",
    val progressDone: Int,
    val progressTotal: Int,
    val videoTitle: String,
    val videoBv: String,
    val videoCoverUrl: String = "",
    val videoIndex: Int,
    val videoTotal: Int,
    val accountName: String,
    val accountStatus: String,
    val accountStatusStyle: TagStyle = TagStyle.OK,
    val accountAvatarUrl: String = "",
    val accountId: Long? = null,
    val actionLine: String,
    val actionLabel: String,
    val actionDone: Int,
    val actionTotal: Int,
    val followed: Int,
    val skipped: Int,
    val failed: Int,
    val paused: Boolean = false,
)

@Composable
fun TaskRunningScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.TASK,
    onBack: () -> Unit = {},
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onLogs: () -> Unit = {},
    onSelectAccount: (Long) -> Unit = {},
    onNeedLogin: () -> Unit = {},
    switchableAccounts: List<AccountItem> = emptyList(),
    onStart: () -> Unit = {},
    upPainter: Painter? = null,
    accountPainter: Painter? = null,
    videoPainter: Painter? = null,
    state: TaskRunningUiState = PreviewMockData.taskRunning,
    modifier: Modifier = Modifier,
) {
    val title = when (state.phase) {
        TaskRunningPhase.IDLE -> "当前无任务"
        TaskRunningPhase.RUNNING -> "任务运行中"
        TaskRunningPhase.PAUSED -> "任务已暂停"
        TaskRunningPhase.FINISHED -> "任务已完成"
        TaskRunningPhase.FAILED -> "任务失败"
        TaskRunningPhase.CANCELLED -> "任务已停止"
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(
            title = title,
            onBack = onBack,
            trailing = {
                DotsMenu(onClick = { onLogs() })
            },
        )
        if (state.phase == TaskRunningPhase.IDLE) {
            IdleTaskState(onStart = onStart, modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = htmlDp(27))
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(htmlDp(8)))
                CurrentTaskCard(state, upPainter)
                CurrentVideoCard(state, videoPainter)
                CurrentAccountCard(
                    state = state,
                    painter = accountPainter,
                    switchableAccounts = switchableAccounts,
                    onSelectAccount = onSelectAccount,
                    onNeedLogin = onNeedLogin,
                )
                CurrentActionCard(state)
                CurrentStatsCard(state)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = htmlDp(18), bottom = htmlDp(18)),
                    horizontalArrangement = Arrangement.spacedBy(htmlDp(20)),
                ) {
                    val primaryRunning = state.phase == TaskRunningPhase.RUNNING ||
                        state.phase == TaskRunningPhase.PAUSED
                    ControlButton(
                        text = when (state.phase) {
                            TaskRunningPhase.RUNNING -> "暂停"
                            TaskRunningPhase.PAUSED -> "继续"
                            else -> "重新开始"
                        },
                        icon = when (state.phase) {
                            TaskRunningPhase.RUNNING -> Icons.Filled.Pause
                            else -> Icons.Filled.PlayArrow
                        },
                        primary = true,
                        onClick = if (primaryRunning) onPause else onStart,
                        modifier = Modifier.weight(1f),
                    )
                    ControlButton(
                        text = "停止",
                        icon = Icons.Filled.Stop,
                        primary = false,
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun IdleTaskState(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = htmlDp(40)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(htmlDp(140))
                .background(
                    Brush.linearGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(htmlDp(56)),
            )
        }
        Text(
            "当前没有正在运行的任务",
            color = Color(BiliColors.NAVY),
            fontSize = htmlSp(30),
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = htmlDp(28)),
        )
        Text(
            "选择目标 UP 与账号后开始自动化任务",
            color = Color(0xFF75859D),
            fontSize = htmlSp(23),
            modifier = Modifier.padding(top = htmlDp(12)),
        )
        PrimaryButton(
            text = "开始任务",
            onClick = onStart,
            modifier = Modifier
                .padding(top = htmlDp(34))
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun CurrentTaskCard(state: TaskRunningUiState, painter: Painter?) {
    BiliCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = htmlDp(25), vertical = htmlDp(24))) {
            Text("当前任务", color = Color(BiliColors.NAVY), fontSize = htmlSp(27), fontWeight = FontWeight.Black)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(21)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(fallbackText = state.upName, size = htmlDp(82), painter = painter, imageUrl = state.upAvatarUrl)
                Column(Modifier.padding(start = htmlDp(19))) {
                    Text(state.upName, color = Color(BiliColors.NAVY), fontSize = htmlSp(27), fontWeight = FontWeight.Black)
                    Text(
                        "UID: ${state.upUid}",
                        color = Color(0xFF72839A),
                        fontSize = htmlSp(22),
                        modifier = Modifier.padding(top = htmlDp(8)),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(22)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("整体进度", color = Color(BiliColors.NAVY), fontSize = htmlSp(25), fontWeight = FontWeight.Black)
                Text(
                    "${state.progressDone} / ${state.progressTotal}",
                    color = Color(0xFF536882),
                    fontSize = htmlSp(23),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(10)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(htmlDp(14)),
            ) {
                ProgressTrack(
                    fraction = if (state.progressTotal == 0) 0f else state.progressDone.toFloat() / state.progressTotal,
                    modifier = Modifier.weight(1f),
                )
                val percent = if (state.progressTotal == 0) 0 else (state.progressDone * 100 / state.progressTotal)
                Text("$percent%", color = Color(0xFF536882), fontSize = htmlSp(22), modifier = Modifier.width(htmlDp(57)))
            }
        }
    }
}

@Composable
private fun CurrentVideoCard(state: TaskRunningUiState, videoPainter: Painter?) {
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(17))) {
        Column(Modifier.padding(horizontal = htmlDp(24), vertical = htmlDp(22))) {
            Text("当前视频", color = Color(BiliColors.NAVY), fontSize = htmlSp(27), fontWeight = FontWeight.Black)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(15)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VideoThumbnail(
                    modifier = Modifier.size(width = htmlDp(202), height = htmlDp(124)),
                    painter = videoPainter,
                    coverUrl = state.videoCoverUrl,
                )
                Column(Modifier.padding(start = htmlDp(19))) {
                    Text(state.videoTitle, color = Color(BiliColors.NAVY), fontSize = htmlSp(24), fontWeight = FontWeight.Black)
                    Text(
                        state.videoBv,
                        color = Color(0xFF71829A),
                        fontSize = htmlSp(20),
                        modifier = Modifier.padding(top = htmlDp(7)),
                    )
                    Text(
                        "${state.videoIndex} / ${state.videoTotal}",
                        color = Color(0xFF71829A),
                        fontSize = htmlSp(20),
                        modifier = Modifier.padding(top = htmlDp(4)),
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    coverUrl: String? = null,
) {
    val url = MediaUrls.httpsFace(coverUrl)
    var remoteBitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            remoteBitmap = com.example.bilibiliautomation.ui.FaceBitmapCache.load(url)
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(htmlDp(11))),
        contentAlignment = Alignment.Center,
    ) {
        when {
            remoteBitmap != null -> {
                Image(
                    bitmap = remoteBitmap!!.asImageBitmap(),
                    contentDescription = "视频封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            painter != null -> {
                Image(
                    painter = painter,
                    contentDescription = "视频封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFDDE7F2), Color(0xFFC9D6E5))),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(htmlDp(46))
                            .background(Color(0x5510213C), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▶", color = Color.White, fontSize = htmlSp(22), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentAccountCard(
    state: TaskRunningUiState,
    painter: Painter?,
    switchableAccounts: List<AccountItem> = emptyList(),
    onSelectAccount: (Long) -> Unit = {},
    onNeedLogin: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(17))) {
        Column(Modifier.padding(horizontal = htmlDp(24), vertical = htmlDp(22))) {
            Text("当前账号", color = Color(BiliColors.NAVY), fontSize = htmlSp(27), fontWeight = FontWeight.Black)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(13)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(fallbackText = state.accountName, size = htmlDp(56), painter = painter, imageUrl = state.accountAvatarUrl)
                Text(
                    state.accountName,
                    color = Color(BiliColors.NAVY),
                    fontSize = htmlSp(25),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = htmlDp(18)).weight(1f),
                )
                Tag(state.accountStatus, state.accountStatusStyle)
                Box(modifier = Modifier.padding(start = htmlDp(14))) {
                    Box(
                        modifier = Modifier
                            .height(htmlDp(40))
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(htmlDp(11)))
                            .clickableNoRipple {
                                if (switchableAccounts.isEmpty()) {
                                    onNeedLogin()
                                } else {
                                    menuExpanded = true
                                }
                            }
                            .padding(horizontal = htmlDp(18)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("切换", color = Color(0xFF5B6F8C), fontSize = htmlSp(21), fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        switchableAccounts.forEach { item ->
                            val selected = item.id == state.accountId
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (selected) "${item.nickname}（当前）" else item.nickname,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    if (!selected) onSelectAccount(item.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentActionCard(state: TaskRunningUiState) {
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(17))) {
        Column(Modifier.padding(horizontal = htmlDp(24), vertical = htmlDp(22))) {
            Text("当前操作", color = Color(BiliColors.NAVY), fontSize = htmlSp(27), fontWeight = FontWeight.Black)
            Text(
                state.actionLine,
                color = Color(0xFF51647F),
                fontSize = htmlSp(22),
                modifier = Modifier.padding(top = htmlDp(13)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(12)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.actionLabel, color = Color(0xFF51647F), fontSize = htmlSp(21))
                Text(
                    "${state.actionDone} / ${state.actionTotal}",
                    color = Color(0xFF51647F),
                    fontSize = htmlSp(21),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(10)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(htmlDp(14)),
            ) {
                ProgressTrack(
                    fraction = if (state.actionTotal == 0) 0f else state.actionDone.toFloat() / state.actionTotal,
                    modifier = Modifier.weight(1f),
                )
                val percent = if (state.actionTotal == 0) 0 else (state.actionDone * 100 / state.actionTotal)
                Text("$percent%", color = Color(0xFF51647F), fontSize = htmlSp(20), modifier = Modifier.width(htmlDp(57)))
            }
        }
    }
}

@Composable
private fun CurrentStatsCard(state: TaskRunningUiState) {
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(17))) {
        Column(Modifier.padding(horizontal = htmlDp(24), vertical = htmlDp(22))) {
            Text("当前数据", color = Color(BiliColors.NAVY), fontSize = htmlSp(27), fontWeight = FontWeight.Black)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(16)),
                horizontalArrangement = Arrangement.spacedBy(htmlDp(12)),
            ) {
                MetricCell("已关注", state.followed.toString(), Color(BiliColors.METRIC_BLUE), Modifier.weight(1f))
                MetricCell("已跳过", state.skipped.toString(), Color(BiliColors.METRIC_NEUTRAL), Modifier.weight(1f))
                MetricCell("失败", state.failed.toString(), Color(BiliColors.METRIC_PINK), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.horizontalGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2)))
    Box(
        modifier = modifier
            .height(htmlDp(81))
            .then(
                if (primary) {
                    Modifier.background(brush, RoundedCornerShape(htmlDp(13)))
                } else {
                    Modifier
                        .background(Color.White, RoundedCornerShape(htmlDp(13)))
                },
            )
            .clickableNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (primary) Color.White else Color(BiliColors.PINK),
                modifier = Modifier.size(htmlDp(26)),
            )
            Text(
                text,
                color = if (primary) Color.White else Color(BiliColors.PINK),
                fontSize = htmlSp(27),
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = htmlDp(14)),
            )
        }
    }
}

@Preview(name = "08 任务运行中", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun TaskRunningScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.HOME) }
    BiliTheme {
        TaskRunningScreen(
            state = PreviewMockData.taskRunning,
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            onBack = {},
            onPause = {},
            onStop = {},
        )
    }
}
