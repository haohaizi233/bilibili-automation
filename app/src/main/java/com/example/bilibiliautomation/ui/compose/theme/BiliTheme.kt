package com.example.bilibiliautomation.ui.compose.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Page 06 HTML `.screen` inner width = phone 687px − 2×9px inset = 669px.
 * Scale = current Compose width (Preview device or emulator) / 669.
 */
class BiliScale(val factor: Float) {
    fun dp(px: Number): Dp = (px.toFloat() * factor).dp
    fun sp(px: Number): TextUnit = (px.toFloat() * factor).sp
}

val LocalBiliScale = staticCompositionLocalOf { BiliScale(360f / 669f) }

@Composable
fun htmlDp(px: Number): Dp = LocalBiliScale.current.dp(px)

@Composable
fun htmlSp(px: Number): TextUnit = LocalBiliScale.current.sp(px)

object BiliDimens {
    const val MOCK_WIDTH = 669f

    val screenHorizontal: Dp @Composable get() = htmlDp(38)
    val statusBarHeight: Dp @Composable get() = htmlDp(70)
    val headerHeight: Dp @Composable get() = htmlDp(117)
    val tabHeight: Dp @Composable get() = htmlDp(110)
    val searchHeight: Dp @Composable get() = htmlDp(83)
    val searchButtonWidth: Dp @Composable get() = htmlDp(143)
    val searchGap: Dp @Composable get() = htmlDp(22)
    val searchRadius: Dp @Composable get() = htmlDp(13)
    val sectionGap: Dp @Composable get() = htmlDp(28)
    val cardHeight: Dp @Composable get() = htmlDp(169)
    val cardGap: Dp @Composable get() = htmlDp(17)
    val cardRadius: Dp @Composable get() = htmlDp(18)
    val cardPadV: Dp @Composable get() = htmlDp(19)
    val cardPadH: Dp @Composable get() = htmlDp(21)
    val avatar: Dp @Composable get() = htmlDp(118)
    val infoGap: Dp @Composable get() = htmlDp(33)
    val radio: Dp @Composable get() = htmlDp(51)
    val confirmHeight: Dp @Composable get() = htmlDp(94)
    val confirmGap: Dp @Composable get() = htmlDp(18)
    val navHeight: Dp @Composable get() = htmlDp(102)
    val navIcon: Dp @Composable get() = htmlDp(33)
    val verifiedBadge: Dp @Composable get() = htmlDp(31)

    val title: TextUnit @Composable get() = htmlSp(36)
    val tab: TextUnit @Composable get() = htmlSp(28)
    val search: TextUnit @Composable get() = htmlSp(25)
    val searchButton: TextUnit @Composable get() = htmlSp(28)
    val name: TextUnit @Composable get() = htmlSp(29)
    val meta: TextUnit @Composable get() = htmlSp(23)
    val confirm: TextUnit @Composable get() = htmlSp(31)
    val navLabel: TextUnit @Composable get() = htmlSp(20)
    val statusTime: TextUnit @Composable get() = htmlSp(22)
}

object BiliColors {
    const val CANVAS_BG = 0xFFF8FBFF
    const val SCREEN_BG = 0xFFFBFCFE
    const val NAVY = 0xFF0F1E38
    const val NAVY_SOFT = 0xFF1C2E4C
    const val MUTED = 0xFF71829B
    const val MUTED_SOFT = 0xFF8C9BB0
    const val PINK = 0xFFFF3478
    const val PINK_2 = 0xFFFF4D86
    const val BLUE = 0xFF2F7DF5
    const val GREEN = 0xFF10B767
    const val ORANGE = 0xFFFF8A00
    const val RED = 0xFFFF1747
    const val LINE = 0xFFE6EBF1
    const val SURFACE = 0xFFFFFFFF
    const val CARD_BORDER = 0xFFEDF1F5
    const val CARD_SHADOW = 0x0B243F5F
    const val TAG_OK_BG = 0xFFE4F9EF
    const val TAG_OK_FG = 0xFF0CAF61
    const val TAG_COOL_BG = 0xFFFFF0E4
    const val TAG_COOL_FG = 0xFFFF6F10
    const val TAG_FAIL_BG = 0xFFFFE8EE
    const val TAG_FAIL_FG = 0xFFFF2455
    const val TAG_IDLE_BG = 0xFFEEF2F6
    const val TAG_IDLE_FG = 0xFF71819A
    const val HOT_BG = 0xFFFFE8F0
    const val INPUT_BG = 0xFFF0F4F8
    const val TRACK_BG = 0xFFE9EEF4
    const val METRIC_BLUE = 0xFFF0F6FF
    const val METRIC_NEUTRAL = 0xFFF4F7FB
    const val METRIC_PINK = 0xFFFFF0F3
    const val NAV_INACTIVE = 0xFF6D8098
    const val FILTER_INFO_BG = 0xFFEDF4FF
    const val FILTER_SUCCESS_BG = 0xFFE8F8EF
    const val FILTER_WARN_BG = 0xFFFFF3E4
    const val FILTER_ERROR_BG = 0xFFFFEAF0
    const val SEARCH_ICON = 0xFF2D405D
    const val TAB_INACTIVE = 0xFF7B899E
    const val RADIO_BORDER = 0xFF94A1B3
    const val SELECTED_CARD = 0xFFFFF3F6
    const val SELECTED_CARD_END = 0xFFFFF7F9
    const val SELECTED_CARD_BORDER = 0xFFFFF0F4
}

private val BiliTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 32.sp, color = androidx.compose.ui.graphics.Color(BiliColors.NAVY)),
    headlineMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 28.sp, color = androidx.compose.ui.graphics.Color(BiliColors.NAVY)),
    titleLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 24.sp, color = androidx.compose.ui.graphics.Color(BiliColors.NAVY)),
    titleMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 20.sp, color = androidx.compose.ui.graphics.Color(BiliColors.NAVY)),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, color = androidx.compose.ui.graphics.Color(BiliColors.NAVY)),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = androidx.compose.ui.graphics.Color(BiliColors.MUTED)),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = androidx.compose.ui.graphics.Color(BiliColors.NAVY)),
)

private val BiliShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun BiliTheme(content: @Composable () -> Unit) {
    val width = LocalConfiguration.current.screenWidthDp.toFloat().coerceAtLeast(320f)
    val scale = remember(width) { BiliScale(width / BiliDimens.MOCK_WIDTH) }
    CompositionLocalProvider(LocalBiliScale provides scale) {
        MaterialTheme(
            typography = BiliTypography,
            shapes = BiliShapes,
            content = content,
        )
    }
}
