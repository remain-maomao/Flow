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
import org.example.flow.engine.ReminderEngine
import org.example.flow.model.ActiveWindow
import org.example.flow.model.BrowserMessage
import org.example.flow.model.ClassificationResult
import org.example.flow.model.Mode
import org.example.flow.monitor.WindowMonitor
import org.example.flow.notify.Notifier
import org.example.flow.server.TabServer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FlowApp(
    tabServer: TabServer,
    reminderEngine: ReminderEngine,
    notifier: Notifier,
    extensionDir: File,
) {
    var activeWindow by remember {
        mutableStateOf(ActiveWindow("(等待采集...)", "(等待采集...)", 0L))
    }
    var browserMessage by remember {
        mutableStateOf<BrowserMessage?>(null)
    }
    var currentMode by remember { mutableStateOf(Mode.WORK) }

    // ── 倍速滑块状态 ──
    var timeScale by remember { mutableStateOf(60L) }

    // ── UI 状态（从 ReminderEngine 收集） ──
    val elapsedVirtualMs by reminderEngine.elapsedVirtualMs.collectAsState()
    val nextReminderVirtualMs by reminderEngine.nextReminderVirtualMs.collectAsState()

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

    // ── 模式检测 + 托盘图标联动 ──
    LaunchedEffect(Unit) {
        val windowResults = WindowMonitor.observeActiveWindow()
            .map { window -> ModeClassifier.classifyWindow(window) }
        val browserResults = tabServer.messages
            .map { msg -> ModeClassifier.classifyBrowser(msg) }
        val merged: kotlinx.coroutines.flow.Flow<ClassificationResult> =
            merge(windowResults, browserResults)

        modeDetector.detect(merged).collectLatest { mode ->
            if (mode != currentMode) {
                currentMode = mode
                reminderEngine.onModeChanged(mode)
                notifier.updateIcon(mode)
            }
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
                Text(modeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = modeColor)
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(modeColor))
            }

            // ── 计时状态 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⏱ 计时状态", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("已持续", ReminderEngine.formatDuration(elapsedVirtualMs))
                    InfoRow(
                        "下次提醒",
                        if (nextReminderVirtualMs != null)
                            ReminderEngine.formatDuration(nextReminderVirtualMs!!)
                        else "—",
                    )
                }
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
                        Text("等待扩展连接...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── 扩展安装引导（仅在扩展未连接时显示） ──
            if (browserMessage == null) {
                ExtensionSetupGuide(extensionDir)
            }

            // ── 时间倍速滑块 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚡ 时间倍速: ${timeScale}x", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = timeScale.toFloat(),
                        onValueChange = { newVal ->
                            val newScale = newVal.toLong().coerceIn(1, 120)
                            if (newScale != timeScale) {
                                timeScale = newScale
                                reminderEngine.updateTimeScale(newScale)
                            }
                        },
                        valueRange = 1f..120f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("1x (真实)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("120x (极速)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── 操作按钮 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { reminderEngine.triggerNow() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("手动触发提醒")
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

// ══════════════════════════════════════════════════════
// 扩展安装引导卡片
// ══════════════════════════════════════════════════════

@Composable
private fun ExtensionSetupGuide(extensionDir: File) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0), // 浅橙色背景
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "🔧 安装浏览器扩展",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "1. 打开 Chrome，地址栏输入 chrome://extensions 回车",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "2. 右上角开启「开发者模式」",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "3. 点击「加载已解压的扩展」→ 选择以下目录：",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            // 路径显示
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF424242),
            ) {
                Text(
                    text = extensionDir.absolutePath,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFA5D6A7),
                )
            }
            Spacer(Modifier.height(8.dp))
            // 打开目录按钮
            OutlinedButton(
                onClick = {
                    try {
                        Runtime.getRuntime().exec(arrayOf("explorer", extensionDir.absolutePath))
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("📂 打开扩展目录")
            }
        }
    }
}

// ══════════════════════════════════════════════════════

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value.ifEmpty { "(空)" },
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}
