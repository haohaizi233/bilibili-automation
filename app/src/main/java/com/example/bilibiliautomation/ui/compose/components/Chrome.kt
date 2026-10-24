package com.example.bilibiliautomation.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliDimens
import com.example.bilibiliautomation.ui.compose.theme.htmlDp

@Composable
fun StatusBar(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars),
    )
}

enum class BiliTab(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    HOME("首页", Icons.Filled.Home, Icons.Outlined.Home),
    ACCOUNT("账号", Icons.Filled.Person, Icons.Outlined.Person),
    TARGET("目标UP", Icons.Filled.Place, Icons.Outlined.Place),
    TASK("任务", Icons.AutoMirrored.Filled.FactCheck, Icons.AutoMirrored.Outlined.FactCheck),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun BottomNav(
    selected: BiliTab,
    onSelect: (BiliTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(htmlDp(1))
                .background(Color(BiliColors.LINE)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BiliDimens.navHeight)
                .padding(start = htmlDp(31), end = htmlDp(31), top = htmlDp(10), bottom = htmlDp(7)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
        BiliTab.entries.forEach { tab ->
            val isActive = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickableNoRipple { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(htmlDp(4)),
            ) {
                Icon(
                    imageVector = if (isActive) tab.selectedIcon else tab.icon,
                    contentDescription = tab.label,
                    tint = if (isActive) Color(BiliColors.PINK) else Color(BiliColors.NAV_INACTIVE),
                    modifier = Modifier.size(BiliDimens.navIcon),
                )
                Text(
                    tab.label,
                    color = if (isActive) Color(BiliColors.PINK) else Color(BiliColors.NAV_INACTIVE),
                    fontSize = BiliDimens.navLabel,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
        }
    }
}
