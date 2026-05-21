package org.example.flow.model

import kotlinx.serialization.Serializable

/**
 * 浏览器扩展通过 WebSocket 发来的标签页信息。
 * 字段一一对应 extension/background.js 中 formatMessage() 的输出。
 */
@Serializable
data class BrowserMessage(
    val type: String,    // "tab_change"
    val url: String,
    val title: String,
    val domain: String,
)
