package org.example.flow.setup

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * 浏览器扩展安装器。
 * 将扩展文件从内嵌内容写入磁盘，并引导用户在 Chrome 中加载。
 */
object ExtensionInstaller {

    private const val EXTENSION_DIR = "extension"

    /** 扩展安装的目标路径（默认在应用同级目录） */
    fun getInstallDir(): File {
        // 尝试几个可能的位置
        val candidates = listOf(
            File(System.getProperty("user.dir"), EXTENSION_DIR),
            File(System.getProperty("compose.application.resources.dir") ?: ".", EXTENSION_DIR),
            File(".").absoluteFile.resolve(EXTENSION_DIR),
        )
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }

    /** 确保扩展文件已安装到磁盘，返回安装目录 */
    fun ensureInstalled(): File {
        val dir = File(System.getProperty("user.dir"), EXTENSION_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
            Files.writeString(dir.toPath().resolve("manifest.json"), MANIFEST_JSON)
            Files.writeString(dir.toPath().resolve("background.js"), BACKGROUND_JS)
            println("[ExtensionInstaller] ✅ 扩展已写入: ${dir.absolutePath}")
        } else {
            println("[ExtensionInstaller] ✅ 扩展已存在: ${dir.absolutePath}")
        }
        return dir
    }

    /** 是否已安装 */
    fun isInstalled(): Boolean {
        val dir = File(System.getProperty("user.dir"), EXTENSION_DIR)
        return dir.exists() && dir.resolve("manifest.json").exists() && dir.resolve("background.js").exists()
    }

    // ══════════════════════════════════════════════════
    // 内嵌的扩展文件内容
    // ══════════════════════════════════════════════════

    private val MANIFEST_JSON = """
{
  "manifest_version": 3,
  "name": "Flow Monitor",
  "version": "0.1.0",
  "description": "将浏览器标签页信息发送给 Flow 桌面应用，用于工作/娱乐模式判断",
  "permissions": ["tabs"],
  "host_permissions": ["<all_urls>"],
  "background": {
    "service_worker": "background.js"
  }
}
""".trimIndent()

    private val BACKGROUND_JS = """
// ── Flow Monitor - Chrome Extension Service Worker ──
// 监听标签页变化，通过 WebSocket 将当前页面信息发送给桌面应用。

const WS_URL = 'ws://localhost:9527';
let ws = null;

function connectWebSocket() {
  try {
    ws = new WebSocket(WS_URL);
    ws.onopen = () => {
      console.log('[Flow] ✅ WebSocket 已连接到桌面应用');
      sendCurrentTab();
    };
    ws.onclose = () => {
      console.log('[Flow] 🔌 WebSocket 断开，3 秒后重连...');
      setTimeout(connectWebSocket, 3000);
    };
    ws.onerror = () => {};
  } catch (e) {
    console.warn('[Flow] WebSocket 创建失败:', e.message);
    setTimeout(connectWebSocket, 3000);
  }
}

function sendToServer(message) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message));
    console.log('[Flow] 📤 已发送:', message.domain, message.title);
  }
}

function formatMessage(tab) {
  let domain = '';
  try { if (tab.url) domain = new URL(tab.url).hostname; } catch (_) { domain = tab.url || ''; }
  return { type: 'tab_change', url: tab.url || '', title: tab.title || '', domain: domain };
}

function sendCurrentTab() {
  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    if (tabs.length > 0) {
      const msg = formatMessage(tabs[0]);
      console.log('[Flow] 📋 当前标签页:', msg.domain, msg.title);
      sendToServer(msg);
    }
  });
}

chrome.tabs.onActivated.addListener((activeInfo) => {
  chrome.tabs.get(activeInfo.tabId, (tab) => {
    const msg = formatMessage(tab);
    console.log('[Flow] 🔄 标签页切换:', msg.domain, msg.title);
    sendToServer(msg);
  });
});

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.url || changeInfo.title) {
    const msg = formatMessage(tab);
    console.log('[Flow] 📝 页面更新:', msg.domain, msg.title);
    sendToServer(msg);
  }
});

console.log('[Flow] 🚀 Flow Monitor 扩展已启动');
connectWebSocket();
""".trimIndent()
}
