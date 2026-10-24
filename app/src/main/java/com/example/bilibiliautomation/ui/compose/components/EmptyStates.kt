package com.example.bilibiliautomation.ui.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.compose.theme.htmlDp
import com.example.bilibiliautomation.ui.compose.theme.htmlSp

enum class EmptyArt { ACCOUNT, TARGET, GROUP, SEARCH, LOG }

@Composable
fun EmptyHint(
    art: EmptyArt,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = htmlDp(24), vertical = htmlDp(36)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyIllustration(art)
        Text(
            title,
            color = Color(BiliColors.NAVY),
            fontSize = htmlSp(28),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = htmlDp(22)),
        )
        Text(
            subtitle,
            color = Color(0xFF75859D),
            fontSize = htmlSp(22),
            textAlign = TextAlign.Center,
            lineHeight = htmlSp(34),
            modifier = Modifier.padding(top = htmlDp(10)),
        )
        if (!actionLabel.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .padding(top = htmlDp(28))
                    .height(htmlDp(64))
                    .background(
                        Brush.horizontalGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                        RoundedCornerShape(htmlDp(13)),
                    )
                    .clickableNoRipple(onClick = onAction)
                    .padding(horizontal = htmlDp(28)),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, color = Color.White, fontSize = htmlSp(24), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun WelcomeGuideDialog(
    onLater: () -> Unit,
    onStart: () -> Unit,
) {
    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = htmlDp(36))
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(htmlDp(24)))
                .padding(horizontal = htmlDp(32), vertical = htmlDp(36))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyIllustration(EmptyArt.TARGET)
            Text(
                "欢迎使用 Bilibili 自动化助手",
                color = Color(BiliColors.NAVY),
                fontSize = htmlSp(30),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = htmlDp(18)),
            )
            Text(
                "帮你把喜欢的 UP 推荐给更多人看。添加自己的账号和目标 UP 后，助手会浏览对方视频、走过评论区，并关注值得关注的人。",
                color = Color(0xFF75859D),
                fontSize = htmlSp(22),
                textAlign = TextAlign.Center,
                lineHeight = htmlSp(34),
                modifier = Modifier.padding(top = htmlDp(12)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(28)),
                verticalArrangement = Arrangement.spacedBy(htmlDp(16)),
            ) {
                GuideStep(1, "添加账号", "用哔哩哔哩 App 扫码登录，Cookie 只保存在本机。")
                GuideStep(2, "添加目标 UP", "输入 UID 找到你想支持的创作者。")
                GuideStep(3, "开始任务", "回到首页点开始，剩下的交给助手。")
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = htmlDp(32))
                    .height(htmlDp(74))
                    .background(
                        Brush.horizontalGradient(listOf(Color(BiliColors.PINK), Color(BiliColors.PINK_2))),
                        RoundedCornerShape(htmlDp(13)),
                    )
                    .clickableNoRipple(onClick = onStart),
                contentAlignment = Alignment.Center,
            ) {
                Text("开始使用", color = Color.White, fontSize = htmlSp(28), fontWeight = FontWeight.Black)
            }
            Text(
                "稍后再说",
                color = Color(0xFF8C9BB0),
                fontSize = htmlSp(22),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = htmlDp(18))
                    .clickableNoRipple(onClick = onLater),
            )
        }
    }
}

@Composable
private fun GuideStep(index: Int, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(htmlDp(44))
                .background(Color(BiliColors.HOT_BG), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$index", color = Color(BiliColors.PINK), fontSize = htmlSp(22), fontWeight = FontWeight.Black)
        }
        Column(Modifier.padding(start = htmlDp(16))) {
            Text(title, color = Color(BiliColors.NAVY), fontSize = htmlSp(24), fontWeight = FontWeight.Black)
            Text(
                body,
                color = Color(0xFF75859D),
                fontSize = htmlSp(20),
                lineHeight = htmlSp(30),
                modifier = Modifier.padding(top = htmlDp(4)),
            )
        }
    }
}

@Composable
private fun EmptyIllustration(art: EmptyArt) {
    val pink = Color(BiliColors.PINK)
    val pinkSoft = Color(BiliColors.HOT_BG)
    val navy = Color(BiliColors.NAVY)
    val blue = Color(BiliColors.BLUE)
    Canvas(modifier = Modifier.size(htmlDp(168))) {
        val w = size.width
        val h = size.height
        drawCircle(color = pinkSoft, radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.52f))
        when (art) {
            EmptyArt.ACCOUNT -> {
                drawCircle(color = Color.White, radius = w * 0.16f, center = Offset(w * 0.38f, h * 0.38f))
                drawCircle(color = pink.copy(alpha = 0.9f), radius = w * 0.16f, center = Offset(w * 0.62f, h * 0.38f))
                drawOval(
                    color = Color.White,
                    topLeft = Offset(w * 0.16f, h * 0.54f),
                    size = Size(w * 0.44f, h * 0.28f),
                )
                drawOval(
                    color = pink.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.40f, h * 0.54f),
                    size = Size(w * 0.44f, h * 0.28f),
                )
            }
            EmptyArt.TARGET -> {
                drawCircle(color = Color.White, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.34f))
                drawCircle(color = pink, radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.34f))
                drawOval(
                    color = pink.copy(alpha = 0.9f),
                    topLeft = Offset(w * 0.24f, h * 0.52f),
                    size = Size(w * 0.52f, h * 0.30f),
                )
            }
            EmptyArt.GROUP -> {
                drawRoundRect(
                    color = blue.copy(alpha = 0.18f),
                    topLeft = Offset(w * 0.22f, h * 0.30f),
                    size = Size(w * 0.56f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.26f, h * 0.36f),
                    size = Size(w * 0.48f, h * 0.34f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                )
                drawLine(
                    color = pink,
                    start = Offset(w * 0.36f, h * 0.48f),
                    end = Offset(w * 0.64f, h * 0.48f),
                    strokeWidth = w * 0.035f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = pink.copy(alpha = 0.5f),
                    start = Offset(w * 0.36f, h * 0.58f),
                    end = Offset(w * 0.56f, h * 0.58f),
                    strokeWidth = w * 0.035f,
                    cap = StrokeCap.Round,
                )
            }
            EmptyArt.SEARCH -> {
                drawCircle(
                    color = Color.White,
                    radius = w * 0.22f,
                    center = Offset(w * 0.44f, h * 0.42f),
                    style = Stroke(width = w * 0.055f),
                )
                drawLine(
                    color = pink,
                    start = Offset(w * 0.60f, h * 0.58f),
                    end = Offset(w * 0.76f, h * 0.74f),
                    strokeWidth = w * 0.07f,
                    cap = StrokeCap.Round,
                )
            }
            EmptyArt.LOG -> {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.28f, h * 0.22f),
                    size = Size(w * 0.44f, h * 0.56f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                )
                drawLine(navy.copy(alpha = 0.25f), Offset(w * 0.36f, h * 0.38f), Offset(w * 0.64f, h * 0.38f), w * 0.03f, StrokeCap.Round)
                drawLine(navy.copy(alpha = 0.25f), Offset(w * 0.36f, h * 0.50f), Offset(w * 0.60f, h * 0.50f), w * 0.03f, StrokeCap.Round)
                drawLine(pink, Offset(w * 0.36f, h * 0.62f), Offset(w * 0.52f, h * 0.62f), w * 0.03f, StrokeCap.Round)
            }
        }
    }
}
