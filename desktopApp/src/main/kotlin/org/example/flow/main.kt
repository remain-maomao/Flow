package org.example.flow

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.flow.engine.ReminderEngine
import org.example.flow.notify.Notifier
import org.example.flow.server.TabServer
import org.example.flow.ui.FlowApp

fun main() = application {
    // 窗口可见性（关闭 → 隐藏到托盘）
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

    // 启动 WebSocket 服务端
    LaunchedEffect(Unit) {
        tabServer.startSafe()
    }

    // 启动提醒引擎
    LaunchedEffect(Unit) {
        reminderEngine.start()
    }

    Window(
        onCloseRequest = { isVisible = false },  // 隐藏到托盘，不退出
        visible = isVisible,
        title = "Flow - 专注助手",
        state = rememberWindowState(width = 480.dp, height = 540.dp),
    ) {
        FlowApp(tabServer, reminderEngine, notifier)
    }
}
