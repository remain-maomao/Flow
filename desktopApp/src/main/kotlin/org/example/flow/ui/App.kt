package org.example.flow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import org.example.flow.model.ActiveWindow
import org.example.flow.model.BrowserMessage
import org.example.flow.monitor.WindowMonitor
import org.example.flow.server.TabServer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FlowApp(tabServer: TabServer) {
    var activeWindow by remember {
        mutableStateOf(ActiveWindow("(等待采集...)", "(等待采集...)", 0L))
    }
    var browserMessage by remember {
        mutableStateOf<BrowserMessage?>(null)
    }

    // 收集桌面窗口变化
    LaunchedEffect(Unit) {
        WindowMonitor.observeActiveWindow().collectLatest { window ->
            activeWindow = window
        }
    }

    // 收集浏览器标签页变化
    LaunchedEffect(Unit) {
        tabServer.messages.collectLatest { msg ->
            browserMessage = msg
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Flow - 窗口监测", style = MaterialTheme.typography.headlineSmall)

            // ── 桌面窗口 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🖥 桌面窗口", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("标题", activeWindow.title)
                    InfoRow("进程", activeWindow.processName)
                }
            }

            // ── 浏览器标签页 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🌐 浏览器标签页", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    if (browserMessage != null) {
                        InfoRow("域名", browserMessage!!.domain)
                        InfoRow("标题", browserMessage!!.title)
                    } else {
                        Text(
                            "等待扩展连接...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── 状态栏 ──
            Text(
                "刷新: ${SimpleDateFormat("HH:mm:ss").format(Date())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = value.ifEmpty { "(空)" },
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}
