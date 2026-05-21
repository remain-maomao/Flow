package org.example.flow

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.flow.engine.ReminderEngine
import org.example.flow.server.TabServer
import org.example.flow.ui.FlowApp

fun main() = application {
    val tabServer = remember { TabServer() }
    val reminderEngine = remember {
        ReminderEngine(
            timeScale = 60L,
            onReminder = { rule ->
                // 第六步：先打印到控制台，第七步再接托盘通知
                println("🔔 提醒: ${rule.message}  [${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}]")
            },
        )
    }

    // 启动 WebSocket 服务端
    LaunchedEffect(Unit) {
        tabServer.startSafe()
    }

    // 启动引擎
    LaunchedEffect(Unit) {
        reminderEngine.start()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Flow - 专注助手",
        state = rememberWindowState(width = 480.dp, height = 520.dp),
    ) {
        FlowApp(tabServer, reminderEngine)
    }
}
