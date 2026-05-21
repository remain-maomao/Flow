package org.example.flow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import org.example.flow.model.ActiveWindow
import org.example.flow.monitor.WindowMonitor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FlowApp() {
    var activeWindow by remember { mutableStateOf(ActiveWindow("(等待采集...)", "(等待采集...)", 0L)) }

    // 在协程中收集窗口变化流
    LaunchedEffect(Unit) {
        WindowMonitor.observeActiveWindow().collectLatest { window ->
            activeWindow = window
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("Flow - 窗口监测", style = MaterialTheme.typography.headlineSmall)

            HorizontalDivider()

            InfoRow(label = "窗口标题", value = activeWindow.title)
            InfoRow(label = "进  程", value = activeWindow.processName)
            InfoRow(
                label = "采集时间",
                value = SimpleDateFormat("HH:mm:ss").format(Date(activeWindow.timestamp)),
            )

            HorizontalDivider()

            Text(
                "切换不同应用窗口，上面的信息会实时更新（每 0.5 秒刷新）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
