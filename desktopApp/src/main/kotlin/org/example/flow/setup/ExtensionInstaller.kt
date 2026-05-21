package org.example.flow.setup

import java.io.File
import java.nio.file.Files

/**
 * 浏览器扩展安装器。
 * 将扩展文件从内嵌内容写入用户目录（%USERPROFILE%/.flow/extension），
 * 避免 Program Files 的写入权限问题。
 */
object ExtensionInstaller {

    private const val EXTENSION_DIR = "extension"

    /** 用户目录下的扩展路径（始终可写） */
    private val userExtensionDir: File by lazy {
        File(System.getProperty("user.home"), ".flow/$EXTENSION_DIR")
    }

    /** 获取扩展安装目录 */
    fun getInstallDir(): File {
        // 优先返回用户目录路径
        if (userExtensionDir.exists()) return userExtensionDir
        // 回退：检查当前目录
        val fallback = File(System.getProperty("user.dir"), EXTENSION_DIR)
        return if (fallback.exists()) fallback else userExtensionDir
    }

    /** 确保扩展文件已安装到磁盘，返回安装目录。失败时返回目录路径但不抛异常 */
    fun ensureInstalled(): File {
        try {
            if (!userExtensionDir.exists()) {
                userExtensionDir.mkdirs()
            }
            // 总是重写文件，确保内容是最新的
            Files.writeString(userExtensionDir.toPath().resolve("manifest.json"), MANIFEST_JSON)
            Files.writeString(userExtensionDir.toPath().resolve("background.js"), BACKGROUND_JS)
            println("[ExtensionInstaller] Written to: ${userExtensionDir.absolutePath}")
        } catch (e: Exception) {
            println("[ExtensionInstaller] Write failed: ${e.message}")
            // 即使写入失败也返回路径，用户可手动创建
        }
        return userExtensionDir
    }

    /** 是否已安装 */
    fun isInstalled(): Boolean {
        return userExtensionDir.exists()
            && userExtensionDir.resolve("manifest.json").exists()
            && userExtensionDir.resolve("background.js").exists()
    }

    // ══════════════════════════════════════════════════
    // 内嵌的扩展文件内容
    // ══════════════════════════════════════════════════

    private val MANIFEST_JSON = """
{
  "manifest_version": 3,
  "name": "Flow Monitor",
  "version": "0.1.0",
  "description": "将浏览器标签页信息发送给 Flow 桌面应用",
  "permissions": ["tabs"],
  "host_permissions": ["<all_urls>"],
  "background": {
    "service_worker": "background.js"
  }
}
""".trimIndent()

    private val BACKGROUND_JS = """
// ── Flow Monitor - Chrome Extension ──

const WS_URL = 'ws://localhost:9527';
let ws = null;

function connectWebSocket() {
  try {
    ws = new WebSocket(WS_URL);
    ws.onopen = () => {
      console.log('[Flow] ✅ 已连接');
      sendCurrentTab();
    };
    ws.onclose = () => {
      console.log('[Flow] 🔌 断开，3秒后重连...');
      setTimeout(connectWebSocket, 3000);
    };
    ws.onerror = () => {};
  } catch (e) {
    setTimeout(connectWebSocket, 3000);
  }
}

function sendToServer(msg) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(msg));
  }
}

function formatMessage(tab) {
  let domain = '';
  try { if (tab.url) domain = new URL(tab.url).hostname; } catch (_) {}
  return { type: 'tab_change', url: tab.url || '', title: tab.title || '', domain };
}

function sendCurrentTab() {
  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    if (tabs.length > 0) sendToServer(formatMessage(tabs[0]));
  });
}

chrome.tabs.onActivated.addListener((info) => {
  chrome.tabs.get(info.tabId, (tab) => sendToServer(formatMessage(tab)));
});

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.url || changeInfo.title) sendToServer(formatMessage(tab));
});

console.log('[Flow] 🚀 扩展已启动');
connectWebSocket();
""".trimIndent()
}
