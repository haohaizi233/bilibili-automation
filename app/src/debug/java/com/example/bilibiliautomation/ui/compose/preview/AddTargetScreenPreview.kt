package com.example.bilibiliautomation.ui.compose.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.example.bilibiliautomation.R
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.screens.AddTargetResult
import com.example.bilibiliautomation.ui.compose.screens.AddTargetScreen
import com.example.bilibiliautomation.ui.compose.screens.AddTargetTab
import com.example.bilibiliautomation.ui.compose.screens.AddTargetUiState
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme

private val page6Results = listOf(
    AddTargetResult("罗翔说刑法", "517327498", "3066.5万", avatarResId = R.drawable.preview_luoxiang),
    AddTargetResult("罗翔老师", "12345678", "102.3万", avatarResId = R.drawable.preview_luoxiang_teacher),
    AddTargetResult("法学罗翔", "87654321", "56.7万", avatarResId = R.drawable.preview_law_user),
    AddTargetResult("罗翔的日常", "11223344", "12.4万", avatarResId = R.drawable.preview_daily),
)

/**
 * Single-page Interactive Preview only.
 * Cross-page navigation is owned by UiShowcaseActivity, not @Preview.
 */
@Composable
internal fun AddTargetScreenPreviewHarness() {
    var activeTab by remember { mutableStateOf(AddTargetTab.SEARCH) }
    var selectedUid by remember { mutableStateOf("517327498") }
    var keyword by remember { mutableStateOf("罗翔") }
    var uidInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(BiliTab.SETTINGS) }
    var confirmMessage by remember { mutableStateOf<String?>(null) }

    AddTargetScreen(
        state = AddTargetUiState(
            activeTab = activeTab,
            keyword = keyword,
            uidInput = uidInput,
            selectedUid = selectedUid,
            results = page6Results,
            confirmMessage = confirmMessage,
            selectedTab = selectedTab,
        ),
        onTabSelect = { selectedTab = it },
        onAddTabSelect = { activeTab = it },
        onKeywordChange = { keyword = it },
        onUidInputChange = { uidInput = it },
        onTargetSelect = { selectedUid = it },
        onSearch = {},
        onConfirm = {
            confirmMessage = if (selectedUid.isBlank()) "请先选择一个 UP" else "已选择 1 个 UP"
        },
        onBack = {},
    )
}

@Preview(name = "06 添加目标UP", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun AddTargetScreenPreview() {
    BiliTheme {
        AddTargetScreenPreviewHarness()
    }
}
