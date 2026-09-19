package com.example.bilibiliautomation.ui.compose.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliDimens
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

@Composable
fun BiliSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = if (checked) Color(BiliColors.BLUE) else Color(0xFFC8D2DE)
    val knobX by animateDpAsState(
        targetValue = if (checked) htmlDp(41) else htmlDp(4),
        animationSpec = tween(150),
        label = "knob",
    )
    Box(
        modifier = modifier
            .width(htmlDp(88))
            .height(htmlDp(49))
            .background(trackColor, RoundedCornerShape(htmlDp(25)))
            .clickableNoRipple { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX, y = htmlDp(4))
                .size(htmlDp(41))
                .shadow(htmlDp(2), CircleShape, ambientColor = Color(0x29000000))
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = BiliDimens.tabHeight,
    horizontalPadding: Dp = htmlDp(37),
    fontSize: TextUnit = BiliDimens.tab,
) {
    Row(
        modifier = modifier
            .height(height)
            .padding(horizontal = horizontalPadding),
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clickableNoRipple { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isActive) Color(BiliColors.NAVY) else Color(BiliColors.TAB_INACTIVE),
                    fontSize = fontSize,
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1,
                )
                if (isActive) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .height(htmlDp(5))
                            .fillMaxWidth()
                            .background(Color(BiliColors.PINK), RoundedCornerShape(htmlDp(5))),
                    )
                }
            }
        }
    }
}
