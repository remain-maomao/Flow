package org.example.flow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
        Column(modifier = Modifier.padding(16.dp)) {
            // ── 标题栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⚙ 黑名单设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) {
                    Text("关闭")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 娱乐域名 ──
            Text("🌐 娱乐域名", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.height(120.dp)) {
                items(config.entertainmentDomains) { domain ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(domain, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            config = config.copy(
                                entertainmentDomains = config.entertainmentDomains - domain,
                            )
                            ConfigManager.save(config)
                            ModeClassifier.reload()
                        }) {
                            Text("✕", color = MaterialTheme.colorScheme.error)
                        }
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
                    placeholder = { Text("输入域名，如 example.com") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val d = newDomain.trim().lowercase()
                    if (d.isNotEmpty() && d !in config.entertainmentDomains) {
                        config = config.copy(entertainmentDomains = config.entertainmentDomains + d)
                        newDomain = ""
                        ConfigManager.save(config)
                        ModeClassifier.reload()
                    }
                }) {
                    Text("添加")
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── 娱乐应用 ──
            Text("🎮 娱乐应用关键词", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.height(120.dp)) {
                items(config.entertainmentApps) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(app, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            config = config.copy(
                                entertainmentApps = config.entertainmentApps - app,
                            )
                        }) {
                            Text("✕", color = MaterialTheme.colorScheme.error)
                        }
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
                    placeholder = { Text("输入关键词，如 steam") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val a = newApp.trim()
                    if (a.isNotEmpty() && a !in config.entertainmentApps) {
                        config = config.copy(entertainmentApps = config.entertainmentApps + a)
                        newApp = ""
                        ConfigManager.save(config)
                        ModeClassifier.reload()
                    }
                }) {
                    Text("添加")
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
        }
    }
}
