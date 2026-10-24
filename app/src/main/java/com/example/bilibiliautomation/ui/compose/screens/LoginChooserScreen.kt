package com.example.bilibiliautomation.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.example.bilibiliautomation.ui.compose.components.BiliCard
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.BottomNav
import com.example.bilibiliautomation.ui.compose.components.StatusBar
import com.example.bilibiliautomation.ui.compose.components.TopHeader
import com.example.bilibiliautomation.ui.compose.components.clickableNoRipple
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliTheme
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

@Composable
fun LoginChooserScreen(
    onTabSelect: (BiliTab) -> Unit,
    selectedTab: BiliTab = BiliTab.ACCOUNT,
    onBack: () -> Unit = {},
    onQrLogin: () -> Unit = {},
    onSmsLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BiliColors.SCREEN_BG)),
    ) {
        StatusBar()
        TopHeader(title = "添加账号", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = htmlDp(34))
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(htmlDp(18)))
            Text(
                "选择登录方式",
                color = Color(BiliColors.NAVY),
                fontSize = htmlSp(32),
                fontWeight = FontWeight.Black,
            )
            Text(
                "账号密码不会经过本应用服务器，一律走哔哩哔哩官方通行证。",
                color = Color(0xFF75859D),
                fontSize = htmlSp(22),
                modifier = Modifier.padding(top = htmlDp(10), bottom = htmlDp(24)),
            )
            LoginMethodCard(
                icon = Icons.Filled.QrCode2,
                title = "扫码登录",
                subtitle = "使用哔哩哔哩 App 扫描二维码",
                onClick = onQrLogin,
            )
            Spacer(Modifier.height(htmlDp(16)))
            LoginMethodCard(
                icon = Icons.Filled.Sms,
                title = "手机验证码登录",
                subtitle = "官方发码；滑块人机验证由 WebView 打开、由你完成",
                onClick = onSmsLogin,
            )
        }
        BottomNav(selected = selectedTab, onSelect = onTabSelect)
    }
}

@Composable
private fun LoginMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    BiliCard(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoRipple(onClick = onClick)
                .padding(horizontal = htmlDp(28), vertical = htmlDp(28)),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Color(BiliColors.PINK), modifier = Modifier.padding(end = htmlDp(20)))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color(BiliColors.NAVY), fontSize = htmlSp(28), fontWeight = FontWeight.Black)
                Text(subtitle, color = Color(0xFF75859D), fontSize = htmlSp(20), modifier = Modifier.padding(top = htmlDp(6)))
            }
        }
    }
}

@Preview(name = "添加账号方式", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun LoginChooserScreenPreview() {
    var selectedTab by remember { mutableStateOf(BiliTab.ACCOUNT) }
    BiliTheme {
        LoginChooserScreen(selectedTab = selectedTab, onTabSelect = { selectedTab = it })
    }
}
