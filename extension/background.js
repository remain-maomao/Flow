// ── Flow Monitor - Chrome Extension Service Worker ──
// 监听标签页变化，通过 WebSocket 将当前页面信息发送给桌面应用。

// ══════════════════════════════════════════════════════
// 1. WebSocket 连接管理
// ══════════════════════════════════════════════════════

const WS_URL = 'ws://localhost:9527';
let ws = null;

function connectWebSocket() {
  try {
    ws = new WebSocket(WS_URL);

    ws.onopen = () => {
      console.log('[Flow] ✅ WebSocket 已连接到桌面应用');
      // 连接成功后立即发送当前标签页信息
      sendCurrentTab();
    };

    ws.onclose = () => {
      console.log('[Flow] 🔌 WebSocket 断开，3 秒后重连...');
      setTimeout(connectWebSocket, 3000);
    };

    ws.onerror = () => {
      // 桌面端未启动是预期情况，不打印错误
      // 连接失败会触发 onclose，由重连逻辑处理
    };
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

// ══════════════════════════════════════════════════════
// 2. 从 Tab 对象提取信息
// ══════════════════════════════════════════════════════

function formatMessage(tab) {
  let domain = '';
  try {
    if (tab.url) {
      domain = new URL(tab.url).hostname;
    }
  } catch (_) {
    domain = tab.url || '';
  }

  return {
    type: 'tab_change',
    url: tab.url || '',
    title: tab.title || '',
    domain: domain,
  };
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

// ══════════════════════════════════════════════════════
// 3. 标签页事件监听
// ══════════════════════════════════════════════════════

// 用户切换到另一个标签页
chrome.tabs.onActivated.addListener((activeInfo) => {
  chrome.tabs.get(activeInfo.tabId, (tab) => {
    const msg = formatMessage(tab);
    console.log('[Flow] 🔄 标签页切换:', msg.domain, msg.title);
    sendToServer(msg);
  });
});

// 当前标签页的 URL 或标题发生变化
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  // 只响应 URL 或标题确实变化的更新
  if (changeInfo.url || changeInfo.title) {
    const msg = formatMessage(tab);
    console.log('[Flow] 📝 页面更新:', msg.domain, msg.title);
    sendToServer(msg);
  }
});

// ══════════════════════════════════════════════════════
// 4. 启动
// ══════════════════════════════════════════════════════

console.log('[Flow] 🚀 Flow Monitor 扩展已启动');
connectWebSocket();
