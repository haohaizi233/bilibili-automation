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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.example.bilibiliautomation.ui.compose.components.EmptyArt
import com.example.bilibiliautomation.ui.compose.components.EmptyHint
import com.example.bilibiliautomation.ui.compose.components.SegmentedTabs
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

data class TargetItem(
    val name: String,
    val uid: String,
    val fans: String,
    val hot: Boolean = false,
    val avatarResId: Int? = null,
    val avatarUrl: String? = null,
)

data class TaskGroupItem(
    val id: Long,
    val name: String,
    val members: List<TargetItem> = emptyList(),
)

@Composable
fun TargetScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.TARGET,
    onAdd: () -> Unit = {},
    onBack: () -> Unit = {},
    onDelete: (String) -> Unit = {},
    targets: List<TargetItem> = PreviewMockData.targetRows.map {
        TargetItem(it.name, it.uid, it.fans, it.hot)
    },
    groups: List<TaskGroupItem> = emptyList(),
    onCreateGroup: (String) -> Unit = { _ -> },
    onRenameGroup: (Long, String) -> Unit = { _, _ -> },
    onDeleteGroup: (Long) -> Unit = { _ -> },
    onAddToGroup: (Long, String) -> Unit = { _, _ -> },
    onRemoveFromGroup: (Long, String) -> Unit = { _, _ -> },
    onRunGroup: (Long) -> Unit = { _ -> },
    onRunTarget: (String) -> Unit = { _ -> },
    modifier: Modifier = Modifier,
) {
    var selectedSegment by remember { mutableIntStateOf(0) }
    var createGroup by remember { mutableStateOf(false) }
    var renameGroup by remember { mutableStateOf<TaskGroupItem?>(null) }
    var addTargetUid by remember { mutableStateOf<String?>(null) }
    var groupNameDraft by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(
            title = "目标UP管理",
            height = htmlDp(105),
            titleSize = htmlSp(34),
            onBack = onBack,
            trailing = {
                val label = if (selectedSegment == 0) "添加" else "新建分组"
                Row(
                    modifier = Modifier
                        .height(htmlDp(64))
                        .background(
                            Brush.linearGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                            RoundedCornerShape(htmlDp(13)),
                        )
                        .clickableNoRipple {
                            if (selectedSegment == 0) onAdd() else {
                                groupNameDraft = ""
                                createGroup = true
                            }
                        }
                        .padding(horizontal = htmlDp(26)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(htmlDp(28)))
                    Text(
                        label,
                        color = Color.White,
                        fontSize = htmlSp(25),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = htmlDp(4)),
                    )
                }
            },
        )
        SegmentedTabs(
            tabs = listOf("全部 (${targets.size})", "分组 (${groups.size})"),
            selected = selectedSegment,
            onSelect = { selectedSegment = it },
            modifier = Modifier.fillMaxWidth(),
            height = htmlDp(82),
            horizontalPadding = htmlDp(27),
            fontSize = htmlSp(25),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(htmlDp(1))
                .background(Color(0xFFEDF1F5)),
        )
        if (selectedSegment == 0) {
            SearchBar(onSearch = onAdd)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = htmlDp(27)),
            ) {
                if (targets.isEmpty()) {
                    item {
                        EmptyHint(
                            art = EmptyArt.TARGET,
                            title = "还没有目标 UP",
                            subtitle = "把你想支持的创作者加进来。之后助手会去 TA 的视频里走走评论区，让更多人看见。",
                            actionLabel = "添加目标 UP",
                            onAction = onAdd,
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                } else {
                    items(targets, key = { it.uid }) { item ->
                        TargetRowCard(
                            item = item,
                            groups = groups,
                            onDelete = onDelete,
                            onRunTarget = onRunTarget,
                            onAddToGroup = { addTargetUid = item.uid },
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = htmlDp(27)),
            ) {
                if (groups.isEmpty()) {
                    item {
                        EmptyHint(
                            art = EmptyArt.GROUP,
                            title = "还没有任务组",
                            subtitle = "把几位目标 UP 放进同一组，就可以一次跑整组，也可以只跑其中一位。",
                            actionLabel = "新建任务组",
                            onAction = {
                                groupNameDraft = ""
                                createGroup = true
                            },
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                } else {
                    items(groups, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            onRunGroup = { onRunGroup(group.id) },
                            onRename = {
                                groupNameDraft = group.name
                                renameGroup = group
                            },
                            onDelete = { onDeleteGroup(group.id) },
                            onRunTarget = onRunTarget,
                            onRemoveMember = { uid -> onRemoveFromGroup(group.id, uid) },
                        )
                    }
                }
                item { Spacer(Modifier.height(htmlDp(18))) }
            }
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
    if (createGroup) {
        GroupNameDialog(
            title = "新建任务组",
            value = groupNameDraft,
            onValueChange = { groupNameDraft = it },
            confirmLabel = "创建",
            onConfirm = {
                onCreateGroup(groupNameDraft)
                createGroup = false
            },
            onDismiss = { createGroup = false },
        )
    }
    renameGroup?.let { group ->
        GroupNameDialog(
            title = "重命名任务组",
            value = groupNameDraft,
            onValueChange = { groupNameDraft = it },
            confirmLabel = "保存",
            onConfirm = {
                onRenameGroup(group.id, groupNameDraft)
                renameGroup = null
            },
            onDismiss = { renameGroup = null },
        )
    }
    addTargetUid?.let { uid ->
        AlertDialog(
            onDismissRequest = { addTargetUid = null },
            title = { Text("添加到任务组") },
            text = {
                if (groups.isEmpty()) {
                    Text("还没有任务组，请先新建。")
                } else {
                    Column {
                        groups.forEach { group ->
                            Text(
                                group.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickableNoRipple {
                                        onAddToGroup(group.id, uid)
                                        addTargetUid = null
                                    }
                                    .padding(vertical = htmlDp(14)),
                                fontWeight = FontWeight.Bold,
                                color = Color(BiliColors.NAVY),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addTargetUid = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun GroupCard(
    group: TaskGroupItem,
    onRunGroup: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRunTarget: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(22))) {
        Column(Modifier.padding(horizontal = htmlDp(20), vertical = htmlDp(18))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(group.name, color = Color(BiliColors.NAVY), fontSize = htmlSp(28), fontWeight = FontWeight.Black)
                    Text(
                        "${group.members.size} 个 UP",
                        color = Color(0xFF71829A),
                        fontSize = htmlSp(21),
                        modifier = Modifier.padding(top = htmlDp(6)),
                    )
                }
                Box(
                    modifier = Modifier
                        .height(htmlDp(52))
                        .background(
                            Brush.linearGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                            RoundedCornerShape(htmlDp(11)),
                        )
                        .clickableNoRipple(onClick = onRunGroup)
                        .padding(horizontal = htmlDp(18)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("跑整组", color = Color.White, fontSize = htmlSp(21), fontWeight = FontWeight.Bold)
                }
                Box {
                    DotsMenu(Modifier.padding(start = htmlDp(8)), onClick = { menuExpanded = true })
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("重命名") },
                            onClick = { menuExpanded = false; onRename() },
                            leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("删除任务组") },
                            onClick = { menuExpanded = false; onDelete() },
                            leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                        )
                    }
                }
            }
            group.members.forEach { member ->
                MemberRow(member, onRunTarget = onRunTarget, onRemove = { onRemoveMember(member.uid) })
            }
        }
    }
}

@Composable
private fun MemberRow(
    item: TargetItem,
    onRunTarget: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = htmlDp(16)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(fallbackText = item.name, size = htmlDp(64), painter = item.avatarResId?.let { painterResource(it) }, imageUrl = item.avatarUrl)
        Column(Modifier.padding(start = htmlDp(16)).weight(1f).widthIn(min = 0.dp)) {
            Text(
                item.name,
                color = Color(BiliColors.NAVY),
                fontSize = htmlSp(24),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("UID: ${item.uid}", color = Color(0xFF71829A), fontSize = htmlSp(18), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            DotsMenu(onClick = { menuExpanded = true })
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("只跑这个 UP") },
                    onClick = { menuExpanded = false; onRunTarget(item.uid) },
                    leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                )
                DropdownMenuItem(
                    text = { Text("移出分组") },
                    onClick = { menuExpanded = false; onRemove() },
                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun TargetRowCard(
    item: TargetItem,
    groups: List<TaskGroupItem>,
    onDelete: (String) -> Unit = {},
    onRunTarget: (String) -> Unit = {},
    onAddToGroup: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    BiliCard(modifier = Modifier.fillMaxWidth().padding(top = htmlDp(22))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(htmlDp(150))
                .padding(horizontal = htmlDp(20), vertical = htmlDp(19)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                fallbackText = item.name,
                size = htmlDp(103),
                painter = item.avatarResId?.let { painterResource(it) },
                imageUrl = item.avatarUrl,
            )
            Column(
                modifier = Modifier
                    .padding(start = htmlDp(24))
                    .weight(1f)
                    .widthIn(min = 0.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.name,
                        color = Color(BiliColors.NAVY),
                        fontSize = htmlSp(28),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.hot) Tag("热门", TagStyle.HOT, Modifier.padding(start = htmlDp(8)))
                }
                Text(
                    "UID: ${item.uid}",
                    color = Color(0xFF71829A),
                    fontSize = htmlSp(21),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = htmlDp(10)),
                )
                Text(
                    "粉丝: ${item.fans}",
                    color = Color(0xFF71829A),
                    fontSize = htmlSp(21),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = htmlDp(4)),
                )
            }
            Box {
                DotsMenu(Modifier.padding(start = htmlDp(4)), onClick = { menuExpanded = true })
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("只跑这个 UP") },
                        onClick = {
                            menuExpanded = false
                            onRunTarget(item.uid)
                        },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(if (groups.isEmpty()) "先新建任务组" else "添加到任务组") },
                        onClick = {
                            menuExpanded = false
                            onAddToGroup()
                        },
                        leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("删除目标") },
                        onClick = {
                            menuExpanded = false
                            onDelete(item.uid)
                        },
                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupNameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                placeholder = { Text("例如：美食区") },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = Color(BiliColors.PINK), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SearchBar(onSearch: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = htmlDp(27), vertical = htmlDp(22))
            .height(htmlDp(72))
            .background(Color(0xFFF1F5F9), RoundedCornerShape(htmlDp(13)))
            .clickableNoRipple { onSearch() }
            .padding(horizontal = htmlDp(22)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF2D405D), modifier = Modifier.size(htmlDp(29)))
        Text(
            "搜索 UID",
            color = Color(0xFF7D8CA1),
            fontSize = htmlSp(23),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = htmlDp(18)),
        )
    }
}

@Preview(name = "05 目标UP管理", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun TargetScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.TARGET) }
    BiliTheme {
        TargetScreen(
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            onAdd = {},
            onBack = {},
            groups = listOf(
                TaskGroupItem(
                    id = 1,
                    name = "法律区",
                    members = PreviewMockData.targetRows.take(2).map { TargetItem(it.name, it.uid, it.fans, it.hot) },
                ),
            ),
        )
    }
}

@Preview(name = "05 目标UP管理 · 空", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun TargetScreenEmptyPreview() {
    BiliTheme {
        TargetScreen(onTabSelect = {}, targets = emptyList(), groups = emptyList())
    }
}
