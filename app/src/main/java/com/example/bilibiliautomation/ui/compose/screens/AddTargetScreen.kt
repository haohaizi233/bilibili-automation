package com.example.bilibiliautomation.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bilibiliautomation.ui.compose.components.Avatar
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.PrimaryButton
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.preview.PreviewMockData
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliDimens
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

enum class AddTargetTab { SEARCH, MANUAL }

data class AddTargetResult(
    val name: String,
    val uid: String,
    val fans: String,
    val verified: Boolean = false,
    val avatarResId: Int? = null,
    val avatarUrl: String? = null,
)

data class AddTargetUiState(
    val activeTab: AddTargetTab = AddTargetTab.SEARCH,
    val keyword: String = "",
    val uidInput: String = "",
    val selectedUid: String = "",
    val results: List<AddTargetResult> = PreviewMockData.searchResults.map {
        AddTargetResult(it.name, it.uid, it.fans, it.verified)
    },
    val confirmMessage: String? = null,
    val confirmIsError: Boolean = false,
    val selectedTab: BiliTab = BiliTab.TARGET,
)

@Composable
fun AddTargetScreen(
    state: AddTargetUiState = AddTargetUiState(),
    onTabSelect: (BiliTab) -> Unit,
    onAddTabSelect: (AddTargetTab) -> Unit = {},
    onKeywordChange: (String) -> Unit = {},
    onUidInputChange: (String) -> Unit = {},
    onTargetSelect: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val canConfirm = state.selectedUid.isNotBlank()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(title = "添加目标UP", onBack = onBack)
        Box(
            Modifier
                .fillMaxWidth()
                .height(htmlDp(1))
                .background(Color(BiliColors.LINE)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = BiliDimens.screenHorizontal),
        ) {
            SearchSection(
                keyword = state.keyword,
                onKeywordChange = onKeywordChange,
                onSearch = onSearch,
            )
            SearchResultList(
                results = state.results,
                selectedUid = state.selectedUid,
                onTargetSelect = onTargetSelect,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = BiliDimens.sectionGap),
            )
        }
        Column(Modifier.padding(horizontal = BiliDimens.screenHorizontal, vertical = BiliDimens.confirmGap)) {
            if (!state.confirmMessage.isNullOrBlank()) {
                Text(
                    state.confirmMessage,
                    color = when {
                        state.confirmIsError -> Color(BiliColors.RED)
                        state.confirmMessage == "正在查询…" -> Color(BiliColors.NAVY)
                        else -> Color(BiliColors.GREEN)
                    },
                    fontSize = BiliDimens.meta,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = htmlDp(10)),
                )
            }
            PrimaryButton(
                text = "添加目标 UP",
                modifier = Modifier.fillMaxWidth(),
                enabled = canConfirm,
                onClick = onConfirm,
            )
        }
        BottomNav(selected = state.selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun SearchSection(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(top = BiliDimens.sectionGap),
        horizontalArrangement = Arrangement.spacedBy(BiliDimens.searchGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(BiliDimens.searchHeight)
                .background(Color(BiliColors.INPUT_BG), RoundedCornerShape(BiliDimens.searchRadius))
                .padding(horizontal = htmlDp(28)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = Color(BiliColors.SEARCH_ICON),
                modifier = Modifier.size(htmlDp(31)),
            )
            Spacer(Modifier.width(htmlDp(20)))
            BasicTextField(
                value = keyword,
                onValueChange = { onKeywordChange(it.filter { ch -> ch.isDigit() }) },
                singleLine = true,
                cursorBrush = SolidColor(Color(BiliColors.PINK)),
                textStyle = TextStyle(
                    color = Color(BiliColors.NAVY),
                    fontSize = BiliDimens.search,
                    fontWeight = FontWeight.Bold,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (keyword.isEmpty()) {
                        Text("输入 UP 主 UID", color = Color(0xFF75859D), fontSize = BiliDimens.search, fontWeight = FontWeight.Bold)
                    }
                    inner()
                },
            )
        }
        Box(
            modifier = Modifier
                .width(BiliDimens.searchButtonWidth)
                .height(BiliDimens.searchHeight)
                .background(
                    Brush.linearGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                    RoundedCornerShape(BiliDimens.searchRadius),
                )
                .clickableNoRipple(onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Text("搜索", color = Color.White, fontSize = BiliDimens.searchButton, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SearchResultList(
    results: List<AddTargetResult>,
    selectedUid: String,
    onTargetSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BiliDimens.cardGap),
    ) {
        items(results, key = { it.uid }) { item ->
            TargetResultCard(
                item = item,
                selected = item.uid == selectedUid,
                onClick = { onTargetSelect(item.uid) },
            )
        }
    }
}

@Composable
private fun TargetResultCard(
    item: AddTargetResult,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(BiliDimens.cardRadius)
    val background = if (selected) {
        Brush.horizontalGradient(listOf(Color(BiliColors.SELECTED_CARD), Color(BiliColors.SELECTED_CARD_END)))
    } else {
        Brush.horizontalGradient(listOf(Color.White, Color.White))
    }
    val border = if (selected) Color(BiliColors.SELECTED_CARD_BORDER) else Color(BiliColors.CARD_BORDER)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BiliDimens.cardHeight)
            .background(background, shape)
            .border(htmlDp(1), border, shape)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = BiliDimens.cardPadH, vertical = BiliDimens.cardPadV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            fallbackText = item.name,
            size = BiliDimens.avatar,
            painter = item.avatarResId?.let { painterResource(it) },
            imageUrl = item.avatarUrl,
            verified = item.verified,
        )
        Spacer(Modifier.width(BiliDimens.infoGap))
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 0.dp),
        ) {
            Text(
                item.name,
                color = Color(BiliColors.NAVY),
                fontSize = BiliDimens.name,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                lineHeight = htmlSp(32),
            )
            Text(
                "UID: ${item.uid}",
                color = Color(BiliColors.MUTED),
                fontSize = BiliDimens.meta,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = htmlDp(11)),
            )
            Text(
                "粉丝: ${item.fans}",
                color = Color(BiliColors.MUTED),
                fontSize = BiliDimens.meta,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = htmlDp(11)),
            )
        }
        Spacer(Modifier.width(htmlDp(12)))
        RadioDot(selected)
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(BiliDimens.radio)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.linearGradient(listOf(Color(0xFFFF2E72), Color(BiliColors.PINK_2))),
                        CircleShape,
                    )
                } else {
                    Modifier.border(htmlDp(3), Color(BiliColors.RADIO_BORDER), CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(htmlDp(22)),
            )
        }
    }
}
