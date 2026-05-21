package org.example.flow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.flow.engine.ReminderEngine
import org.example.flow.model.ActiveWindow
import org.example.flow.model.BrowserMessage
import org.example.flow.model.Mode
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard 面板：模式指示、计时、窗口监测、浏览器标签、扩展引导、开发者工具。
 */
@Composable
fun DashboardPanel(
    activeWindow: ActiveWindow,
    browserMessage: BrowserMessage?,
    currentMode: Mode,
    elapsedVirtualMs: Long,
    nextReminderVirtualMs: Long?,
    timeScale: Long,
    developerMode: Boolean,
    showSetupGuide: Boolean,
    extensionDir: File,
    reminderEngine: ReminderEngine,
    onTimeScaleChange: (Long) -> Unit,
) {
    val modeColor = if (currentMode == Mode.WORK) Color(0xFF4CAF50) else Color(0xFFF44336)
    val modeLabel = if (currentMode == Mode.WORK) "Working" else "Entertainment"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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

        // ── Timer Status ──
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Timer", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                InfoRow("Elapsed", ReminderEngine.formatDuration(elapsedVirtualMs))
                InfoRow(
                    "Next Reminder",
                    if (nextReminderVirtualMs != null) ReminderEngine.formatDuration(nextReminderVirtualMs) else "--",
                )
            }
        }

        // ── Desktop Window ──
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Desktop Window", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                InfoRow("Title", activeWindow.title)
                InfoRow("Process", activeWindow.processName)
            }
        }

        // ── Browser Tab ──
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Browser Tab", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                browserMessage?.let { msg ->
                    InfoRow("Domain", msg.domain)
                    InfoRow("Title", msg.title)
                } ?: run {
                    Text("Waiting for extension...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Extension Setup Guide ──
        if (showSetupGuide) {
            ExtensionSetupGuide(extensionDir)
        }

        // ── TimeScale Slider (Developer Mode only) ──
        if (developerMode) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Time Scale: ${timeScale}x", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = timeScale.toFloat(),
                        onValueChange = { newVal ->
                            onTimeScaleChange(newVal.toLong().coerceIn(1, 120))
                        },
                        valueRange = 1f..120f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("1x (Real)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("120x (Fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Manual Trigger (Developer Mode only) ──
            Button(
                onClick = { reminderEngine.triggerNow() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Trigger Reminder Now")
            }
        }

        // ── Status Bar ──
        Text(
            "Updated: ${SimpleDateFormat("HH:mm:ss").format(Date())}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

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
            text = value.ifEmpty { "(empty)" },
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ══════════════════════════════════════════════════════
// Extension Setup Guide
// ══════════════════════════════════════════════════════

@Composable
private fun ExtensionSetupGuide(extensionDir: File) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Setup Browser Extension", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("1. Open Chrome and go to chrome://extensions", style = MaterialTheme.typography.bodySmall)
            Text("2. Enable 'Developer mode' (top right)", style = MaterialTheme.typography.bodySmall)
            Text("3. Click 'Load unpacked' and select:", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
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
            OutlinedButton(
                onClick = {
                    try { Runtime.getRuntime().exec(arrayOf("explorer", extensionDir.absolutePath)) } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Folder")
            }
        }
    }
}
