package org.example.flow

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.graphics.ImageBitmap
import org.example.flow.engine.ReminderEngine
import org.example.flow.model.Mode
import org.example.flow.notify.EmojiPicker
import org.example.flow.notify.NotificationPopup
import org.example.flow.notify.Notifier
import org.example.flow.server.TabServer
import org.example.flow.setup.ExtensionInstaller
import org.example.flow.setup.IconGenerator
import org.example.flow.ui.FlowApp
import flow.desktopapp.generated.resources.Res
import flow.desktopapp.generated.resources.flow
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    val extensionConnectedState = remember { mutableStateOf(false) }
    var extensionConnected by extensionConnectedState

    val notifier = remember {
        Notifier(
            onShowWindow = { isVisible = true },
            onExit = { exitApplication() },
        )
    }

    // 通知弹窗状态
    data class NotificationState(val message: String, val mode: Mode, val image: ImageBitmap?)
    var notification by remember { mutableStateOf<NotificationState?>(null) }

    // 使用 holder 模式避免 lambda 自引用
    val engineHolder = remember { arrayOfNulls<ReminderEngine>(1) }

    val reminderEngine = remember {
        ReminderEngine(
            timeScale = 60L,
            onReminder = { rule ->
                val mode = engineHolder[0]?.getCurrentMode() ?: Mode.WORK
                val config = org.example.flow.classify.ConfigManager.load()
                val image = if (config.emojiFolder.isNotBlank()) {
                    EmojiPicker.pick(config.emojiFolder)
                } else null
                notification = NotificationState(rule.message, mode, image)
            },
        ).also { engineHolder[0] = it }
    }

    val tabServer = remember {
        TabServer(
            onConnected = { extensionConnected = true },
            onDisconnected = { extensionConnected = false },
        )
    }

    // 扩展目录（使用用户目录，确保可写）
    val extensionDir = remember { ExtensionInstaller.getInstallDir() }

    // 异步初始化
    LaunchedEffect(Unit) {
        // 扩展安装
        ExtensionInstaller.ensureInstalled()
        println("[main] Extension dir: ${extensionDir.absolutePath}")

        // 首次启动时生成图标
        IconGenerator.generateIfMissing()
    }

    // 启动 WebSocket 服务端
    LaunchedEffect(Unit) {
        tabServer.startSafe()
    }

    // 启动提醒引擎
    LaunchedEffect(Unit) {
        reminderEngine.start()
    }

    // ── 通知弹窗 ──
    if (notification != null) {
        val n = notification!!
        NotificationPopup(
            message = n.message,
            mode = n.mode,
            image = n.image,
            visible = true,
            onDismiss = { notification = null },
        )
    }

    // ── 窗口图标（从 compose 资源加载） ──
    val windowIcon = painterResource(Res.drawable.flow)

    Window(
        onCloseRequest = { isVisible = false },
        visible = isVisible,
        title = "Flow",
        icon = windowIcon,
        state = rememberWindowState(width = 680.dp, height = 500.dp),
    ) {
        FlowApp(
            tabServer = tabServer,
            reminderEngine = reminderEngine,
            notifier = notifier,
            extensionDir = extensionDir,
            extensionConnected = extensionConnectedState,
        )
    }
}
