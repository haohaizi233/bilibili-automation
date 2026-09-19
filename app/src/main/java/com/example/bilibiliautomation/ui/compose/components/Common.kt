package com.example.bilibiliautomation.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bilibiliautomation.ui.FaceBitmapCache
import com.example.bilibiliautomation.util.MediaUrls
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.BiliDimens
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

@Composable
fun BiliCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(BiliDimens.cardRadius))
            .border(htmlDp(1), Color(BiliColors.CARD_BORDER), RoundedCornerShape(BiliDimens.cardRadius))
            .shadow(
                elevation = htmlDp(6),
                shape = RoundedCornerShape(BiliDimens.cardRadius),
                ambientColor = Color(BiliColors.CARD_SHADOW),
                spotColor = Color(BiliColors.CARD_SHADOW),
            ),
    ) { content() }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val brush = Brush.horizontalGradient(
        listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2)),
    )
    Box(
        modifier = modifier
            .height(BiliDimens.confirmHeight)
            .background(brush, RoundedCornerShape(BiliDimens.searchRadius))
            .clickableNoRipple(enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = BiliDimens.confirm, fontWeight = FontWeight.Black)
    }
}

@Composable
fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .height(htmlDp(74))
            .background(Color.White, RoundedCornerShape(htmlDp(13)))
            .border(htmlDp(2), Color(BiliColors.PINK), RoundedCornerShape(htmlDp(13)))
            .clickableNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color(BiliColors.PINK), fontSize = htmlSp(27), fontWeight = FontWeight.Black)
    }
}

enum class TagStyle(val fg: Long, val bg: Long) {
    OK(BiliColors.TAG_OK_FG, BiliColors.TAG_OK_BG),
    COOL(BiliColors.TAG_COOL_FG, BiliColors.TAG_COOL_BG),
    FAIL(BiliColors.TAG_FAIL_FG, BiliColors.TAG_FAIL_BG),
    IDLE(BiliColors.TAG_IDLE_FG, BiliColors.TAG_IDLE_BG),
    HOT(BiliColors.PINK, BiliColors.HOT_BG),
}

@Composable
fun Tag(text: String, style: TagStyle, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(style.bg), RoundedCornerShape(htmlDp(10)))
            .padding(horizontal = htmlDp(14), vertical = htmlDp(8)),
    ) {
        Text(text, color = Color(style.fg), fontSize = htmlSp(20), fontWeight = FontWeight.Black, lineHeight = htmlSp(20))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color(BiliColors.NAVY),
        fontSize = htmlSp(26),
        fontWeight = FontWeight.Black,
        modifier = modifier,
    )
}

@Composable
fun MetaText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color(BiliColors.MUTED),
        fontSize = htmlSp(20),
        fontWeight = FontWeight.Medium,
        modifier = modifier,
    )
}

@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    gradient: List<Color> = listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2)),
    trackColor: Color = Color(BiliColors.TRACK_BG),
) {
    Box(
        modifier = modifier
            .height(htmlDp(17))
            .background(trackColor, RoundedCornerShape(htmlDp(9))),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(htmlDp(17))
                .background(Brush.horizontalGradient(gradient), RoundedCornerShape(htmlDp(9))),
        )
    }
}

@Composable
fun MetricCell(
    label: String,
    value: String,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(htmlDp(96))
            .background(background, RoundedCornerShape(htmlDp(13)))
            .padding(top = htmlDp(14)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color(BiliColors.NAVY_SOFT), fontSize = htmlSp(21), fontWeight = FontWeight.Medium)
        Text(
            value,
            color = Color(BiliColors.NAVY),
            fontSize = htmlSp(32),
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = htmlDp(7)),
        )
    }
}

@Composable
fun DotsMenu(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val dots: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(htmlDp(5)),
        ) {
            repeat(3) {
                Box(Modifier.size(htmlDp(4)).background(Color(BiliColors.NAVY), CircleShape))
            }
        }
    }
    if (onClick != null) {
        Box(
            modifier = modifier
                .size(48.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { dots() }
    } else {
        Box(modifier = modifier.width(htmlDp(28)), contentAlignment = Alignment.Center) { dots() }
    }
}

@Composable
fun Avatar(
    fallbackText: String,
    modifier: Modifier = Modifier,
    size: Dp = BiliDimens.avatar,
    painter: Painter? = null,
    imageUrl: String? = null,
    verified: Boolean = false,
) {
    val faceUrl = MediaUrls.httpsFace(imageUrl)
    var remoteBitmap by remember(faceUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(faceUrl) {
        val loaded = FaceBitmapCache.load(faceUrl)
        remoteBitmap = loaded
        com.example.bilibiliautomation.logging.AutomationLogger.i(
            "avatar fallbackText=${fallbackText.take(1)} imageUrl=$imageUrl faceUrl=$faceUrl loaded=${loaded != null}",
        )
    }
    Box(modifier = modifier.size(size)) {
        when {
            remoteBitmap != null -> {
                Image(
                    bitmap = remoteBitmap!!.asImageBitmap(),
                    contentDescription = fallbackText,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
            painter != null -> {
                Image(
                    painter = painter,
                    contentDescription = fallbackText,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFFEDF3F9), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        fallbackText.take(1),
                        color = Color(BiliColors.BLUE),
                        fontSize = (size.value * 0.38f).sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        if (verified) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(BiliDimens.verifiedBadge)
                    .background(Color(BiliColors.GREEN), CircleShape)
                    .border(htmlDp(3), Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = Color.White, fontSize = htmlSp(18), fontWeight = FontWeight.Black, lineHeight = htmlSp(18))
            }
        }
    }
}

@Composable
fun AvatarPlaceholder(
    size: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Avatar(
        fallbackText = label,
        modifier = modifier,
        size = htmlDp(size),
    )
}
