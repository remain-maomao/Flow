package org.example.flow

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.flow.server.TabServer
import org.example.flow.ui.FlowApp

fun main() = application {
    val tabServer = remember { TabServer() }

    // 启动 WebSocket 服务端
    LaunchedEffect(Unit) {
        tabServer.startSafe()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Flow - 专注助手",
        state = rememberWindowState(width = 480.dp, height = 380.dp),
    ) {
        FlowApp(tabServer)
    }
}
