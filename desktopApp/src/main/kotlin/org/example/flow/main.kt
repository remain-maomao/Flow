package org.example.flow

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.flow.engine.ReminderEngine
import org.example.flow.notify.Notifier
import org.example.flow.server.TabServer
import org.example.flow.setup.ExtensionInstaller
import org.example.flow.ui.FlowApp
import java.io.File

fun main() = application {
    var isVisible by remember { mutableStateOf(true) }

    val notifier = remember {
        Notifier(
            onShowWindow = { isVisible = true },
            onExit = { exitApplication() },
        )
    }

    val reminderEngine = remember {
        ReminderEngine(
            timeScale = 60L,
            onReminder = { rule -> notifier.show("Flow", rule.message) },
        )
    }

    val tabServer = remember { TabServer() }

    // 启动时自动释放扩展文件到磁盘
    val extensionDir = remember { ExtensionInstaller.ensureInstalled() }
    LaunchedEffect(Unit) {
        println("[main] 扩展已安装到: ${extensionDir.absolutePath}")
    }

    // 启动 WebSocket 服务端
    LaunchedEffect(Unit) {
        tabServer.startSafe()
    }

    // 启动提醒引擎
    LaunchedEffect(Unit) {
        reminderEngine.start()
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
