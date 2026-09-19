package com.example.bilibiliautomation.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliDimens
import com.example.bilibiliautomation.ui.compose.theme.htmlDp

/**
 * Wraps screen content in a phone bezel with a notch camera and status bar.
 * Designed to mirror the HTML reference mockups.
 */
@Composable
fun PhoneFrame(
    modifier: Modifier = Modifier,
    showCamera: Boolean = true,
    content: @Composable () -> Unit,
) {
    val bezel = Brush.linearGradient(
        listOf(
            Color(0xFFE6EBEF),
            Color(0xFF9097A0),
            Color(0xFF101319),
            Color(0xFF020304),
            Color(0xFF06080B),
            Color(0xFF2A3037),
            Color(0xFFD9DEE3),
        ),
    )
    Box(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(78.dp), spotColor = Color(0x33435CB8))
            .background(bezel, RoundedCornerShape(78.dp)),
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFFBFCFE))),
                    RoundedCornerShape(63.dp),
                ),
        ) {
            if (showCamera) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 19.dp)
                        .size(31.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF56708D), Color(0xFF0E1721), Color(0xFF020406)),
                            ),
                            CircleShape,
                        ),
                )
            }
            Column(Modifier.fillMaxSize()) { content() }
        }
    }
}

@Composable
fun TopHeader(
    title: String,
    modifier: Modifier = Modifier,
    height: Dp = BiliDimens.headerHeight,
    titleSize: TextUnit = BiliDimens.title,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = BiliDimens.screenHorizontal, vertical = htmlDp(8)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(htmlDp(43))
                    .clickableNoRipple { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "返回",
                    tint = Color(BiliColors.NAVY),
                    modifier = Modifier.size(htmlDp(34)),
                )
            }
            Box(Modifier.size(htmlDp(20)))
        }
        Text(title, color = Color(BiliColors.NAVY), fontSize = titleSize, fontWeight = FontWeight.Black)
        Box(Modifier.weight(1f))
        trailing?.invoke()
    }
}
