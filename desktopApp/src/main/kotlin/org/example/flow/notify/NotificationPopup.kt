package org.example.flow.notify

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import kotlinx.coroutines.delay
import org.example.flow.model.Mode
import java.awt.Toolkit

/**
 * 自定义通知弹窗。支持两种模式：
 * - 表情包模式：透明背景 + 随机图片 + 文字
 * - 文字模式：深灰圆角卡片（降级）
 */
@Composable
fun NotificationPopup(
    message: String,
    mode: Mode,
    image: ImageBitmap?,
    onDismiss: () -> Unit,
    visible: Boolean,
) {
    if (!visible) return

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val hasImage = image != null
    val popupWidth = if (hasImage) 200 else 300
    val popupHeight = if (hasImage) 240 else 80
    val x = screenSize.width - popupWidth - 20
    val y = screenSize.height - popupHeight - 50

    val windowState = rememberWindowState(
        size = DpSize(popupWidth.dp, popupHeight.dp),
        position = WindowPosition(x = x.dp, y = y.dp),
    )

    val modeColor = if (mode == Mode.WORK) Color(0xFF4CAF50) else Color(0xFFF44336)

    LaunchedEffect(message) {
        delay(4_000)
        onDismiss()
    }

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        undecorated = true,
        alwaysOnTop = true,
        transparent = true,
        focusable = false,
        resizable = false,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(500)),
        ) {
            if (hasImage) {
                @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                EmojiCard(message, modeColor, image!!)
            } else {
                TextCard(message, modeColor)
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// 表情包模式：透明背景 + 图片 + 文字
// ══════════════════════════════════════════════════════

@Composable
private fun EmojiCard(message: String, modeColor: Color, image: ImageBitmap) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 图片（缩放适配，保持比例）
        Image(
            painter = BitmapPainter(image),
            contentDescription = "emoji",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(8.dp))
        // 文字带阴影（白色加黑边，透明背景上可读）
        Text(
            text = message,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

// ══════════════════════════════════════════════════════
// 文字模式：深灰卡片（降级）
// ══════════════════════════════════════════════════════

@Composable
private fun TextCard(message: String, modeColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2D2D2D)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(modeColor),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
        }
    }
}
