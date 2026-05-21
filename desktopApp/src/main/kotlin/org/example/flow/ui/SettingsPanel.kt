package org.example.flow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.flow.classify.AppConfig
import org.example.flow.classify.ConfigManager
import org.example.flow.classify.ModeClassifier

/**
 * 设置面板：编辑娱乐黑名单（域名 + 应用关键词）。
 * 整个面板可上下滚动。
 */
@Composable
fun SettingsPanel(onClose: () -> Unit) {
    var config by remember { mutableStateOf(ConfigManager.load()) }
    var newDomain by remember { mutableStateOf("") }
    var newApp by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── 标题栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Blacklist Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 娱乐域名 ──
            Text("Entertainment Domains", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            config.entertainmentDomains.forEach { domain ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(domain, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        config = config.copy(entertainmentDomains = config.entertainmentDomains - domain)
                        ConfigManager.save(config); ModeClassifier.reload()
                    }) {
                        Text("X", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            // 添加域名
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newDomain,
                    onValueChange = { newDomain = it },
                    placeholder = { Text("e.g. example.com") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val d = newDomain.trim().lowercase()
                    if (d.isNotEmpty() && d !in config.entertainmentDomains) {
                        config = config.copy(entertainmentDomains = config.entertainmentDomains + d)
                        newDomain = ""
                        ConfigManager.save(config); ModeClassifier.reload()
                    }
                }) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── 娱乐应用 ──
            Text("Entertainment Apps", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            config.entertainmentApps.forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        config = config.copy(entertainmentApps = config.entertainmentApps - app)
                        ConfigManager.save(config); ModeClassifier.reload()
                    }) {
                        Text("X", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            // 添加应用
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newApp,
                    onValueChange = { newApp = it },
                    placeholder = { Text("e.g. steam") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val a = newApp.trim()
                    if (a.isNotEmpty() && a !in config.entertainmentApps) {
                        config = config.copy(entertainmentApps = config.entertainmentApps + a)
                        newApp = ""
                        ConfigManager.save(config); ModeClassifier.reload()
                    }
                }) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 恢复默认 ──
            OutlinedButton(
                onClick = {
                    config = AppConfig()
                    ConfigManager.save(config)
                    ModeClassifier.reload()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore Defaults")
            }

            // 底部留白确保滚动时最后一项可见
            Spacer(Modifier.height(16.dp))
        }
    }
}
