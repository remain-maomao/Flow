package org.example.flow

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.flow.engine.ReminderEngine
import org.example.flow.model.Mode
import org.example.flow.notify.NotificationPopup
import org.example.flow.notify.Notifier
import org.example.flow.server.TabServer
import org.example.flow.setup.ExtensionInstaller
import org.example.flow.ui.FlowApp

fun main() = application {
    var isVisible by remember { mutableStateOf(true) }

    val notifier = remember {
        Notifier(
            onShowWindow = { isVisible = true },
            onExit = { exitApplication() },
        )
    }

    // 通知弹窗状态：message + mode
    var notification by remember { mutableStateOf<Pair<String, Mode>?>(null) }

    // 使用 holder 模式避免 lambda 自引用
    val engineHolder = remember { arrayOfNulls<ReminderEngine>(1) }

    val reminderEngine = remember {
        ReminderEngine(
            timeScale = 60L,
            onReminder = { rule ->
                val mode = engineHolder[0]?.getCurrentMode() ?: Mode.WORK
                notification = Pair(rule.message, mode)
            },
        ).also { engineHolder[0] = it }
    }

    val tabServer = remember { TabServer() }

    // 扩展目录（使用用户目录，确保可写）
    val extensionDir = remember { ExtensionInstaller.getInstallDir() }

    // 异步初始化：扩展安装先于服务启动，失败不崩溃
    LaunchedEffect(Unit) {
        ExtensionInstaller.ensureInstalled()
        println("[main] 扩展目录: ${extensionDir.absolutePath}")
    }

    // 启动 WebSocket 服务端
    LaunchedEffect(Unit) {
        tabServer.startSafe()
    }

    // 启动提醒引擎
    LaunchedEffect(Unit) {
        reminderEngine.start()
    }

    // ── 通知弹窗（在 notification 不为 null 时渲染） ──
    if (notification != null) {
        NotificationPopup(
            message = notification!!.first,
            mode = notification!!.second,
            visible = notification != null,
            onDismiss = { notification = null },
        )
    }

    Window(
        onCloseRequest = { isVisible = false },
        visible = isVisible,
        title = "Flow - 专注助手",
        state = rememberWindowState(width = 500.dp, height = 640.dp),
    ) {
        FlowApp(
            tabServer = tabServer,
            reminderEngine = reminderEngine,
            notifier = notifier,
            extensionDir = extensionDir,
        )
    }
}
