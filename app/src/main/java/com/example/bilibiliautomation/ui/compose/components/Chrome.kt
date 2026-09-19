package com.example.bilibiliautomation.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliDimens
import com.example.bilibiliautomation.ui.compose.theme.htmlDp

@Composable
fun StatusBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BiliDimens.statusBarHeight)
            .padding(start = htmlDp(48), end = htmlDp(48), top = htmlDp(23)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text("9:41", color = Color(0xFF111111), fontSize = BiliDimens.statusTime, fontWeight = FontWeight.Black)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(htmlDp(9))) {
            SignalIcon()
            WifiIcon()
            BatteryIcon()
        }
    }
}

@Composable
private fun SignalIcon() {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(19.dp),
    ) {
        listOf(7, 11, 15, 19).forEach { h ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(h.dp)
                    .background(Color(0xFF080808), RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun WifiIcon() {
    Box(
        Modifier.size(width = 23.dp, height = 17.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .size(width = 21.dp, height = 12.dp)
                .background(Color(0xFF080808), RoundedCornerShape(50)),
        )
        Box(
            Modifier
                .size(7.dp)
                .background(Color(0xFF080808), RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun BatteryIcon() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 20.dp, height = 20.dp)
                .background(Color(0xFF080808), RoundedCornerShape(3.dp)),
        )
        Box(
            Modifier
                .size(width = 3.dp, height = 8.dp)
                .background(Color(0xFF080808), RoundedCornerShape(0)),
        )
    }
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
            .background(Color.White),
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
