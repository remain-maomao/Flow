package org.example.flow.server

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import org.example.flow.model.BrowserMessage
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

/**
 * WebSocket 服务端，监听 localhost:9527。
 * 接收浏览器扩展发来的标签页信息，通过 SharedFlow 暴露给 UI 层。
 */
class TabServer : WebSocketServer(InetSocketAddress(9527)) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _messages = MutableSharedFlow<BrowserMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<BrowserMessage> = _messages

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        println("[TabServer] ✅ 浏览器已连接: ${conn.remoteSocketAddress}")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        try {
            val msg = json.decodeFromString<BrowserMessage>(message)
            println("[TabServer] 📨 收到: domain=${msg.domain}, title=${msg.title}")
            _messages.tryEmit(msg)
        } catch (e: Exception) {
            println("[TabServer] ⚠️ 消息解析失败: ${e.message}")
        }
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        println("[TabServer] 🔌 浏览器已断开: $reason")
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        println("[TabServer] ❌ 错误: ${ex.message}")
    }

    override fun onStart() {
        println("[TabServer] 🚀 WebSocket 服务已启动: ws://localhost:9527")
    }

    /** 启动服务端，端口占用时打印警告但不崩溃 */
    fun startSafe() {
        try {
            start()
        } catch (e: Exception) {
            println("[TabServer] ⚠️ 启动失败（端口可能被占用）: ${e.message}")
        }
    }
}
