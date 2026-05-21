package org.example.flow.notify

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import kotlinx.coroutines.delay
import org.example.flow.model.Mode
import java.awt.Toolkit

/**
 * 自定义通知弹窗：右下角圆角卡片，替代 Windows 原生气泡。
 *
 * @param message 提醒文案，如 "💧 该喝水了"
 * @param mode 当前模式，决定色条颜色
 * @param onDismiss 关闭回调
 * @param visible 控制显示/隐藏
 */
@Composable
fun NotificationPopup(
    message: String,
    mode: Mode,
    onDismiss: () -> Unit,
    visible: Boolean,
) {
    if (!visible) return

    // 计算屏幕右下角位置
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val popupWidth = 300
    val popupHeight = 80
    val x = screenSize.width - popupWidth - 20
    val y = screenSize.height - popupHeight - 50

    val windowState = rememberWindowState(
        size = DpSize(popupWidth.dp, popupHeight.dp),
        position = WindowPosition(x = x.dp, y = y.dp),
    )

    val modeColor = if (mode == Mode.WORK) Color(0xFF4CAF50) else Color(0xFFF44336)

    // 4 秒后自动消失
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
            NotificationCard(message, modeColor)
        }
    }
}

// ══════════════════════════════════════════════════════
// 通知卡片 UI
// ══════════════════════════════════════════════════════

@Composable
private fun NotificationCard(message: String, modeColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2D2D2D)), // 深灰色背景
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧色条（4dp 宽）
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(modeColor),
            )
            Spacer(Modifier.width(12.dp))
            // 文案
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
