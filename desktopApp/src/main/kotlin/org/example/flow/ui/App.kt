package org.example.flow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.example.flow.classify.ModeClassifier
import org.example.flow.classify.ModeDetector
import org.example.flow.model.ActiveWindow
import org.example.flow.model.BrowserMessage
import org.example.flow.model.ClassificationResult
import org.example.flow.model.Mode
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
    var currentMode by remember { mutableStateOf(Mode.WORK) }

    val modeDetector = remember { ModeDetector(debounceMs = 5_000L) }

    // ── 桌面窗口数据采集 ──
    LaunchedEffect(Unit) {
        WindowMonitor.observeActiveWindow().collectLatest { window ->
            activeWindow = window
        }
    }

    // ── 浏览器标签页数据采集 ──
    LaunchedEffect(Unit) {
        tabServer.messages.collectLatest { msg ->
            browserMessage = msg
        }
    }

    // ── 模式检测（两路数据 → 分类 → 防抖） ──
    LaunchedEffect(Unit) {
        // 将两路原始数据各自分类为 ClassificationResult，然后合并
        val windowResults = WindowMonitor.observeActiveWindow()
            .map { window -> ModeClassifier.classifyWindow(window) }

        val browserResults = tabServer.messages
            .map { msg -> ModeClassifier.classifyBrowser(msg) }

        val mergedResults: kotlinx.coroutines.flow.Flow<ClassificationResult> =
            merge(windowResults, browserResults)

        modeDetector.detect(mergedResults).collectLatest { mode ->
            currentMode = mode
        }
    }

    // ── UI ────────────────────────────────────────────

    val modeColor = if (currentMode == Mode.WORK) Color(0xFF4CAF50) else Color(0xFFF44336)
    val modeLabel = if (currentMode == Mode.WORK) "工作模式" else "娱乐模式"

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Flow - 专注助手", style = MaterialTheme.typography.headlineSmall)

            // ── 模式指示条 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(modeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = modeColor,
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(modeColor),
                )
            }

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
