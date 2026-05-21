package org.example.flow

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.flow.ui.FlowApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Flow - 专注助手",
        state = rememberWindowState(width = 500.dp, height = 300.dp),
    ) {
        FlowApp()
    }
}
