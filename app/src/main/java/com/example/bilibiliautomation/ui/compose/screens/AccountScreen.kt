package com.example.bilibiliautomation.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

enum class AccountStatus(val label: String, val style: TagStyle) {
    OK("正常", TagStyle.OK),
    COOLING("冷却中", TagStyle.COOL),
    FAILED("登录失效", TagStyle.FAIL),
    IDLE("未使用", TagStyle.IDLE),
}

data class AccountItem(
    val id: Long = 0L,
    val name: String,
    val nickname: String,
    val fans: String,
    val following: String = "—",
    val status: AccountStatus,
    val lastUsed: String? = null,
    val cooldown: String? = null,
    val avatarResId: Int? = null,
    val avatarUrl: String? = null,
    val compact: Boolean = false,
    val isCurrent: Boolean = false,
)

@Composable
fun AccountScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.ACCOUNT,
    onAdd: () -> Unit = {},
    onBack: () -> Unit = {},
    onSetCurrent: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    currentAccountId: Long? = null,
    accounts: List<AccountItem> = PreviewMockData.accountRows.map { row ->
        AccountItem(
            name = row.name,
            nickname = row.nickname,
            fans = row.fans,
            status = when (row.status) {
                PreviewMockData.AccountStatus.OK -> AccountStatus.OK
                PreviewMockData.AccountStatus.COOLING -> AccountStatus.COOLING
                PreviewMockData.AccountStatus.FAILED -> AccountStatus.FAILED
                PreviewMockData.AccountStatus.IDLE -> AccountStatus.IDLE
            },
            lastUsed = row.lastUsed,
            cooldown = row.cooldown,
            compact = row.lastUsed == null && row.cooldown == null,
        )
    },
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(
            title = "账号管理",
            height = htmlDp(112),
            titleSize = htmlSp(34),
            onBack = onBack,
            trailing = {
                Row(
                    modifier = Modifier
                        .height(htmlDp(64))
                        .background(
                            Brush.linearGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                            RoundedCornerShape(htmlDp(13)),
                        )
                        .clickableNoRipple { onAdd() }
                        .padding(horizontal = htmlDp(24)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(htmlDp(28)))
                    Text(
                        "添加账号",
                        color = Color.White,
                        fontSize = htmlSp(25),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = htmlDp(4)),
                    )
                }
            },
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(27)),
            verticalArrangement = Arrangement.spacedBy(htmlDp(17)),
            contentPadding = PaddingValues(vertical = htmlDp(8)),
        ) {
            if (accounts.isEmpty()) {
                item {
                    EmptyHint(
                        art = EmptyArt.ACCOUNT,
                        title = "还没有可用账号",
                        subtitle = "扫码登录哔哩哔哩后，助手才能替你浏览视频、走过评论区。账号信息只保存在这台手机上。",
                        actionLabel = "添加第一个账号",
                        onAction = onAdd,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
            } else {
                items(accounts, key = { if (it.id != 0L) it.id else it.name.hashCode().toLong() }) { item ->
                    AccountCard(item, isCurrent = item.id == currentAccountId, onSetCurrent = onSetCurrent, onDelete = onDelete)
                }
            }
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun AccountCard(
    item: AccountItem,
    isCurrent: Boolean = false,
    onSetCurrent: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    BiliCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = htmlDp(20), end = htmlDp(21), top = htmlDp(20), bottom = htmlDp(19)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Avatar(
                    fallbackText = item.nickname,
                    size = htmlDp(96),
                    painter = item.avatarResId?.let { painterResource(it) },
                    imageUrl = item.avatarUrl,
                )
                Box(
                    Modifier
                        .size(htmlDp(28))
                        .background(
                            when (item.status) {
                                AccountStatus.OK -> Color(0xFF10BF66)
                                AccountStatus.FAILED -> Color(0xFFFF315D)
                                else -> Color(0xFFFF8A00)
                            },
                            CircleShape,
                        ),
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = htmlDp(23))
                    .weight(1f)
                    .widthIn(min = 0.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.nickname,
                        color = Color(BiliColors.NAVY),
                        fontSize = htmlSp(28),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCurrent) {
                        Text(
                            "当前",
                            color = Color.White,
                            fontSize = htmlSp(18),
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .padding(end = htmlDp(8))
                                .background(
                                    Brush.linearGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                                    RoundedCornerShape(htmlDp(6)),
                                )
                                .padding(horizontal = htmlDp(8), vertical = htmlDp(2)),
                        )
                    }
                    Tag(item.status.label, item.status.style)
                }
                if (item.name.isNotBlank() && item.name != item.nickname) {
                    Text(
                        "UID ${item.name}",
                        color = Color(0xFF6F819C),
                        fontSize = htmlSp(20),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = htmlDp(8)),
                    )
                }
                Text(
                    "关注：${item.following}  粉丝：${item.fans}",
                    color = Color(0xFF6F819C),
                    fontSize = htmlSp(20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = htmlDp(8)),
                )
                item.cooldown?.let {
                    Text(
                        it,
                        color = Color(0xFFFF6810),
                        fontSize = htmlSp(20),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        modifier = Modifier.padding(top = htmlDp(8)),
                    )
                }
            }
            Box {
                DotsMenu(Modifier.padding(start = htmlDp(4)), onClick = { menuExpanded = true })
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("设为当前账号") },
                        onClick = {
                            menuExpanded = false
                            onSetCurrent(item.id)
                        },
                        leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("删除账号") },
                        onClick = {
                            menuExpanded = false
                            onDelete(item.id)
                        },
                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                    )
                }
            }
        }
    }
}

@Preview(name = "02 账号管理", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun AccountScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.ACCOUNT) }
    BiliTheme {
        AccountScreen(selectedTab = selectedTab, onTabSelect = { selectedTab = it }, onAdd = {}, onBack = {})
    }
}

@Preview(name = "02 账号管理 · 空", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun AccountScreenEmptyPreview() {
    BiliTheme {
        AccountScreen(onTabSelect = {}, accounts = emptyList())
    }
}
