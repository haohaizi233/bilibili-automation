package com.example.bilibiliautomation.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
    modifier: Modifier = Modifier,
) {
    var selectedSegment by remember { mutableIntStateOf(0) }
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
                Row(
                    modifier = Modifier
                        .height(htmlDp(64))
                        .background(
                            Brush.linearGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                            RoundedCornerShape(htmlDp(13)),
                        )
                        .clickableNoRipple { onAdd() }
                        .padding(horizontal = htmlDp(26)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(htmlDp(28)))
                    Text(
                        "添加",
                        color = Color.White,
                        fontSize = htmlSp(25),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = htmlDp(4)),
                    )
                }
            },
        )
        SegmentedTabs(
            tabs = listOf("全部 (${targets.size})", "分组"),
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
        SearchBar(onSearch = onAdd)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(27)),
        ) {
            items(targets, key = { it.uid }) { item ->
                TargetRowCard(item, onDelete = onDelete)
            }
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun TargetRowCard(item: TargetItem, onDelete: (String) -> Unit = {}) {
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
        TargetScreen(selectedTab = selectedTab, onTabSelect = { selectedTab = it }, onAdd = {}, onBack = {})
    }
}
