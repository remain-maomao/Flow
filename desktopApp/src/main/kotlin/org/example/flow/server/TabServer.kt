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
        println("[TabServer] Browser connected: ${conn.remoteSocketAddress}")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        try {
            val msg = json.decodeFromString<BrowserMessage>(message)
            println("[TabServer] Received: domain=${msg.domain}, title=${msg.title}")
            _messages.tryEmit(msg)
        } catch (e: Exception) {
            println("[TabServer] Parse error: ${e.message}")
        }
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        println("[TabServer] Browser disconnected: $reason")
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        println("[TabServer] Error: ${ex.message}")
    }

    override fun onStart() {
        println("[TabServer] Server started: ws://localhost:9527")
    }

    /** 启动服务端，端口占用时打印警告但不崩溃 */
    fun startSafe() {
        try {
            start()
        } catch (e: Exception) {
            println("[TabServer] Start failed: ${e.message}")
        }
    }
}
