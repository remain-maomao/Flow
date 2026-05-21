# Flow - 最小可行 DEMO 实施计划

## 项目概述

桌面软件，监测当前活跃窗口变化和浏览器 TAB 变化，判断用户在工作模式或娱乐模式，执行不同提醒策略。

- **工作模式**：每 15min 提醒喝水，每 40min 提醒站立
- **娱乐模式**：每 2min 提醒一次（概率助推：P(退出)=0.1 → 30min 内 79% 退出概率）
- **演示加速**：60x 时间倍速（1 秒 = 1 分钟），可调节

## 架构

```
┌─────────────────────────────────────────────┐
│          Compose Desktop App (Kotlin)        │
│                                             │
│  WindowMonitor ──┐                          │
│  (JNA 桌面窗口)   ├──→ ModeClassifier ──→ ReminderEngine ──→ Notifier
│  WebSocketServer ─┘                          │
│  (接收浏览器扩展)                              │
└──────────────────┬──────────────────────────┘
                   │ ws://localhost:9527
┌──────────────────┴──────────────────────────┐
│        Chrome Extension (JavaScript)         │
│  监听 tabs.onActivated / tabs.onUpdated      │
│  发送: { url, title, domain }                │
└─────────────────────────────────────────────┘
```

---

## 分步计划

### ✅ 第一步：项目结构搭建与基础依赖 【已完成】

**目标**：清理模板代码，建立目录结构，添加 JNA + WebSocket 依赖，验证可编译运行。

**已完成**：

| 子步骤 | 内容 | 成果 |
|--------|------|------|
| 1.1 | 删除 Greeting.kt、GreetingUtil.kt、Platform.kt、Platform.jvm.kt | shared 模块精简，App.kt 变为空壳 |
| 1.2 | 创建 7 个包：model/ monitor/ classify/ engine/ server/ notify/ ui/ | 目录结构就绪 |
| 1.3 | 添加 JNA 5.14 + Java-WebSocket 1.5.6 + kotlinx-serialization 1.8.1 | libs.versions.toml + build.gradle.kts 更新 |
| 1.4 | 重写 main.kt，启动 Compose 窗口显示骨架信息 | `./gradlew :desktopApp:run` 正常启动 |

**验收通过**：`./gradlew :desktopApp:compileKotlin` → BUILD SUCCESSFUL，`./gradlew :desktopApp:run` → 窗口正常弹出。

---

### ✅ 第二步：窗口监测模块 【已完成】

**目标**：通过 JNA 调用 Windows API，轮询获取前台窗口标题和进程名，并在 UI 面板实时显示。

---

#### 2.1 定义 `ActiveWindow` 数据类

**产出文件**：`desktopApp/src/main/kotlin/org/example/flow/model/ActiveWindow.kt`

**具体代码**：
```kotlin
package org.example.flow.model

data class ActiveWindow(
    val title: String,          // 窗口标题
    val processName: String,    // 进程名，如 "idea64.exe"
    val timestamp: Long,        // 采集时间戳
)
```

**验证**：`./gradlew :desktopApp:compileKotlin` 通过

---

#### 2.2 编写 JNA 接口 `Win32.kt`

**产出文件**：`desktopApp/src/main/kotlin/org/example/flow/monitor/Win32.kt`

**具体内容**：
- `User32` 接口 — `GetForegroundWindow()`, `GetWindowTextW()`, `GetWindowThreadProcessId()`
- `Kernel32` 接口 — `OpenProcess()`, `CloseHandle()`
- `Psapi` 接口 — `GetModuleBaseNameW()`
- 常量：`PROCESS_QUERY_INFORMATION = 0x0400`, `PROCESS_VM_READ = 0x0010`
- 使用 `com.sun.jna.platform.win32.WinDef.HWND` / `WinNT.HANDLE`

**验证**：`./gradlew :desktopApp:compileKotlin` 通过

---

#### 2.3 编写 `WindowMonitor`

**产出文件**：`desktopApp/src/main/kotlin/org/example/flow/monitor/WindowMonitor.kt`

**具体逻辑**：
1. `fun observeActiveWindow(): Flow<ActiveWindow>` 用 callbackFlow 创建冷流
2. 循环：`User32.GetForegroundWindow()` → 提取标题 + 进程名 → `trySend(ActiveWindow(...))` → `delay(500)`
3. `getWindowTitle(hwnd)`: `CharArray(512)` → `GetWindowTextW` → `String(buffer).trim()`
4. `getProcessName(hwnd)`: `GetWindowThreadProcessId` → `OpenProcess` → `GetModuleBaseNameW` → `CloseHandle`，异常返回 `"unknown"`

**验证**：`./gradlew :desktopApp:compileKotlin` 通过

---

#### 2.4 集成到 UI 实时显示

**产出文件**：`ui/App.kt`（新建）+ `main.kt`（修改）

**具体逻辑**：
- `ui/App.kt`：`FlowApp()` composable，LaunchedEffect 收集 Flow，显示标题+进程名+时间戳
- `main.kt`：Window 内调用 `FlowApp()`

**验收标准（手动操作）**：
1. `./gradlew :desktopApp:run` 启动窗口
2. 切换到浏览器 → UI 显示浏览器标题和 `chrome.exe`
3. 切换到 IDE → UI 变为新窗口信息
4. 切换到记事本 → UI 显示 `notepad.exe`
5. 标题和进程名每 0.5 秒刷新

---

### ✅ 第三步：浏览器扩展开发 【已完成】

**目标**：Chrome 扩展监听标签页变化，通过 WebSocket 发送 URL/标题/域名给桌面应用。

> 注意：扩展是独立组件，不参与 Gradle 编译。验证全部通过浏览器手动操作。

---

#### 3.1 创建 `extension/manifest.json`

**产出文件**：`extension/manifest.json`

**具体内容（逐字段说明）**：
```json
{
  "manifest_version": 3,          // Chrome 要求 V3
  "name": "Flow Monitor",
  "version": "0.1.0",
  "description": "将浏览器标签页信息发送给 Flow 桌面应用",
  "permissions": ["tabs"],        // 允许读取所有标签页 URL
  "host_permissions": ["<all_urls>"],  // 允许读取任意域名
  "background": {
    "service_worker": "background.js"  // 后台 Service Worker
  }
}
```

**验证**：
- 打开 Chrome → `chrome://extensions/` → 开启「开发者模式」→ 「加载已解压的扩展」→ 选择 `extension/` 目录
- 扩展出现在列表中，无红色错误提示

---

#### 3.2 编写 `background.js` — 监听标签页变化

**产出文件**：`extension/background.js`

**具体逻辑（两段监听器）**：

```js
// 监听器 1：用户切换到另一个标签页
chrome.tabs.onActivated.addListener((activeInfo) => {
  chrome.tabs.get(activeInfo.tabId, (tab) => {
    logTabInfo(tab);
    sendToServer(formatMessage(tab));
  });
});

// 监听器 2：当前标签页的 URL 或标题发生变化
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.url || changeInfo.title) {
    logTabInfo(tab);
    sendToServer(formatMessage(tab));
  }
});
```

**`formatMessage(tab)` 产出格式**：
```json
{
  "type": "tab_change",
  "url": "https://www.bilibili.com/video/BV1xx411c7mD",
  "title": "【4K】xxx - bilibili",
  "domain": "www.bilibili.com"
}
```

**验证**：
- 加载扩展后 → 打开 `chrome://extensions/` → 点击「Service Worker」链接 → 打开 DevTools Console
- 切换标签页 / 刷新页面 → Console 打印：`[Flow] tab_change: { url: "...", title: "...", domain: "..." }`

---

#### 3.3 加入 WebSocket 客户端

**产出文件**：`extension/background.js`（在 3.2 的基础上增加）

**具体逻辑**：

```js
let ws = null;

function connectWebSocket() {
  ws = new WebSocket('ws://localhost:9527');
  ws.onopen = () => console.log('[Flow] WebSocket 已连接');
  ws.onclose = () => {
    console.log('[Flow] WebSocket 断开，3 秒后重连...');
    setTimeout(connectWebSocket, 3000);
  };
  ws.onerror = (err) => console.warn('[Flow] WebSocket 错误 (桌面端可能未启动):', err);
}

function sendToServer(message) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message));
    console.log('[Flow] 已发送:', message);
  } else {
    console.log('[Flow] WebSocket 未连接，跳过发送');
  }
}

// 启动时立即连接
connectWebSocket();
```

**验证**：
- 桌面端未启动 → Console 显示 `WebSocket 错误...` + `3 秒后重连...`
- 后续第四步启动桌面 WebSocket 服务端后 → Console 自动变为 `WebSocket 已连接` + `已发送: { ... }`

---

**验收标准（第四步联调前可独立验证的部分）**：
1. 扩展能加载到 Chrome，无报错 ✅
2. 切换标签页 / URL 变化 → Service Worker Console 打印 `[Flow] tab_change: {url, title, domain}` ✅
3. WebSocket 连接尝试发起（桌面端未启动时显示错误 + 自动重连）✅

---

### ✅ 第四步：WebSocket 服务端 【已完成】

**目标**：桌面端启动 WebSocket Server 监听 `localhost:9527`，接收浏览器扩展发来的 JSON → 反序列化为 Kotlin 数据类 → 和 WindowMonitor 合并为统一窗口流 → UI 同时展示两端数据。

---

#### 4.1 创建 `BrowserMessage` 数据类

**产出文件**：`desktopApp/src/main/kotlin/org/example/flow/model/BrowserMessage.kt`

**字段一一对应扩展发来的 JSON**：
```kotlin
@Serializable
data class BrowserMessage(
    val type: String,    // "tab_change"
    val url: String,
    val title: String,
    val domain: String,
)
```

**验证**：`./gradlew :desktopApp:compileKotlin` 通过

---

#### 4.2 编写 `TabServer` — WebSocket 服务端

**产出文件**：`desktopApp/src/main/kotlin/org/example/flow/server/TabServer.kt`

**具体逻辑（逐行可写）**：
1. 继承 `org.java_websocket.server.WebSocketServer(InetSocketAddress(9527))`
2. 持有 `private val _messages = MutableSharedFlow<BrowserMessage>(extraBufferCapacity = 8)`
3. 暴露 `val messages: SharedFlow<BrowserMessage> = _messages`
4. `onOpen()` → 打印日志 `"浏览器已连接"`
5. `onMessage(conn, message: String)` → `Json.decodeFromString<BrowserMessage>(message)` → `_messages.tryEmit(msg)`
6. `onClose()` → 打印日志 `"浏览器已断开"`
7. `onError()` → 打印异常
8. `fun start()` → 调用 `super.start()`，包裹 try-catch（端口占用时打印警告）

**关键 import**：
- `org.java_websocket.server.WebSocketServer`
- `org.java_websocket.WebSocket`
- `org.java_websocket.handshake.ClientHandshake`
- `kotlinx.serialization.json.Json`

**验证**：`./gradlew :desktopApp:compileKotlin` 通过

---

#### 4.3 整合：main.kt 启动服务 + UI 合并双源显示

**修改文件**：
- `main.kt`：在 `application {}` 内启动 `TabServer`
- `ui/App.kt`：同时显示桌面窗口 + 浏览器标签页信息

**main.kt 新增逻辑**：
```kotlin
val tabServer = remember { TabServer() }
LaunchedEffect(Unit) { tabServer.start() }
```

**ui/App.kt 新增逻辑**：
- 新增 `browserMessage` 状态，通过参数接收 TabServer 的 SharedFlow 或在 FlowApp 内部收集
- UI 分为两栏：左侧「桌面窗口」、右侧「浏览器标签页」
- 桌面窗口信息来源：`WindowMonitor.observeActiveWindow()`（已有）
- 浏览器信息来来源：`TabServer.messages`（新增）

**验收标准（手动操作）**：
1. `./gradlew :desktopApp:run` 启动桌面应用
2. Chrome 扩展的 Service Worker Console 显示 `[Flow] ✅ WebSocket 已连接到桌面应用`
3. 桌面 UI 右侧「浏览器标签页」区域显示：域名 + 页面标题
4. 切换浏览器标签页 → 桌面 UI 同步更新
5. 关闭浏览器 → 桌面 UI 显示「浏览器已断开」
6. 重新打开浏览器 → 自动重连，UI 恢复

---

### ✅ 第五步：模式分类器 【已完成】

**目标**：接收 `ActiveWindow` + `BrowserMessage`，输出 `Mode.WORK` 或 `Mode.ENTERTAINMENT`。默认工作模式，只维护娱乐黑名单。5 秒防抖避免快速抖动。

---

#### 5.1 创建 `Mode` 枚举 + `ClassificationResult` 数据类

**产出文件**：
- `model/Mode.kt`
- `model/ClassificationResult.kt`

**具体代码**：
```kotlin
enum class Mode { WORK, ENTERTAINMENT }

data class ClassificationResult(
    val mode: Mode,
    val matchedKeyword: String?,  // 命中的娱乐关键词，调试用
    val timestamp: Long,
)
```

**验���**：`./gradlew :desktopApp:compileKotlin` 通过

---

#### 5.2 编写 `ModeClassifier`

**产出文件**：`classify/ModeClassifier.kt`

**核心逻辑（纯函数，无状态）**：

```kotlin
object ModeClassifier {
    // 娱乐域名黑名单（浏览器扩展发来的 domain 字段）
    private val entertainmentDomains = listOf(
        "youtube.com", "bilibili.com", "netflix.com",
        "iqiyi.com", "douyin.com", "twitch.tv",
        "douyu.com", "huya.com", "reddit.com",
        "twitter.com", "x.com", "weibo.com",
        "zhihu.com", "steampowered.com", "epicgames.com",
    )

    // 娱乐应用标题关键词（桌面窗口标题匹配）
    private val entertainmentTitleKeywords = listOf(
        "steam", "epic games", "riot", "原神", "崩坏",
        "起点", "晋江", "漫画", "bilibili", "抖音",
    )

    fun classify(activeWindow: ActiveWindow): ClassificationResult
    fun classifyBrowser(msg: BrowserMessage): ClassificationResult
}
```

**分类优先级**：
1. 如果有 BrowserMessage → 用 `domain` 匹配 `entertainmentDomains`
2. 否则用桌面窗口 `title` 匹配 `entertainmentTitleKeywords`
3. 都不匹配 → `Mode.WORK`

**验证**：`./gradlew :desktopApp:compileKotlin` 通过

---

#### 5.3 编写 `ModeDetector` — 带 5 秒防抖

**产出文件**：`classify/ModeDetector.kt`

**核心逻辑**：
- 接收 `Flow<ClassificationResult>` → 输出稳定的 `Flow<Mode>`
- 防抖：只有当当前分类连续 5 秒都是 ENTERTAINMENT 且当前模式是 WORK 时，才切换到 ENTERTAINMENT
- 同理：连续 5 秒都是 WORK 且当前模式是 ENTERTAINMENT 时，才切回 WORK
- 内部维护 `lastEntertainmentTime` 和 `lastWorkTime` 两个时间戳

```kotlin
class ModeDetector(private val debounceMs: Long = 5000L) {
    fun detect(input: Flow<ClassificationResult>): Flow<Mode>
}
```

**验证**：`./gradlew :desktopApp:compileKotlin` 通过

---

#### 5.4 集成到 UI

**修改文件**：`ui/App.kt`

**具体变更**：
- 在 `FlowApp()` 中创建 `val modeDetector = remember { ModeDetector() }`
- 将 `WindowMonitor` 和 `tabServer.messages` 合并为统一的 `ClassificationResult` 流
- 经过 `modeDetector.detect()` 得到稳定 `Mode`
- UI 新增一行显示当前模式，WORK 显示绿色，ENTERTAINMENT 显示红色

**验收标准（手动操作）**：
1. 默认显示「工作模式」（绿色）
2. 浏览器打开 bilibili.com → 等待 5 秒 → UI 变为「娱乐模式」（红色），显示匹配关键词 `bilibili.com`
3. 关闭 bilibili 标签页，切回 VS Code → 等待 5 秒 → UI 恢复「工作模式」
4. 5 秒内快速来回切换 → 模式不抖动

---

### ✅ 第六步：提醒引擎 【已完成】

**目标**：模式驱动的定时器，支持演示时间加速。模式切换时重置所有计时器。

---

#### 6.1 定义提醒规则数据结构

**产出文件**：`engine/ReminderRule.kt`

**具体代码**：
```kotlin
data class ReminderRule(
    val id: String,           // 唯一标识，如 "drink_water"
    val intervalMs: Long,     // 触发间隔（毫秒），如 15 * 60_000 = 15 分钟
    val message: String,      // 提醒文案，如 "💧 该喝水了"
    val priority: Int = 0,    // 优先级，数值越大越优先（多个提醒同时触发时取最高）
)
```

**验证**：编译通过

---

#### 6.2 编写 `ReminderEngine` — 核心状态机

**产出文件**：`engine/ReminderEngine.kt`

**具体逻辑（逐行为）**：

```kotlin
class ReminderEngine(
    private val timeScale: Long = 60L,  // 演示加速：60x，1 秒 = 1 分钟
    private val onReminder: (String) -> Unit,  // 提醒回调（后续接 Notifier）
) {
    // 工作模式规则
    private val workRules = listOf(
        ReminderRule("drink", 15 * 60_000, "💧 该喝水了", 0),
        ReminderRule("stand", 40 * 60_000, "🧍 站起来活动一下", 1),
    )
    // 娱乐模式规则
    private val entRules = listOf(
        ReminderRule("ent_nudge", 2 * 60_000, "⏰ 已经过去 2 分钟了", 0),
    )

    private var currentMode: Mode = Mode.WORK
    private var modeStartTime: Long = 0L
    private val timerJobs = mutableMapOf<String, Job>()

    fun onModeChanged(newMode: Mode) { ... }
    fun getElapsedInMode(): Long { ... }
    fun getNextReminderIn(): Long? { ... }
}
```

**核心机制**：
- `onModeChanged(mode)` → 取消所有旧定时器 → 记录 `modeStartTime` → 为每个规则启动协程 `delay(interval / timeScale)` → 触发 `onReminder` → 重新调度
- `getElapsedInMode()` → `(now - modeStartTime) * timeScale`（虚拟时间）
- `getNextReminderIn()` → 最近一个即将触发的提醒距离现在的虚拟时间

**验证**：编译通过

---

#### 6.3 添加时间倍速调节

**产出文件**：`engine/ReminderEngine.kt`（同 6.2，增加 `updateTimeScale` 方法）

**具体逻辑**：
```kotlin
fun updateTimeScale(newScale: Long) {
    // 取消所有 timer，用新的 timeScale 重新调度
}
```

**验证**：编译通过

---

#### 6.4 集成到 UI — 显示时长 + 倒计时

**修改文件**：`ui/App.kt`、`main.kt`

**具体变更**：
- 在 `FlowApp` 中创建 `ReminderEngine` 实例
- 监听 `currentMode` 变化 → 调用 `engine.onModeChanged(mode)`
- UI 新增行：
  - 「已持续: XX 分 XX 秒」（虚拟时间）
  - 「下次提醒: XX 秒后」
- 提醒触发时先 `println()` 打印到控制台（第七步再接入托盘通知）

**验收标准（@60x 加速，手动操作）**：
1. 应用启动 → 工作模式 → 控制台约 15 秒后打印 `💧 该喝水了`
2. 再过约 25 秒 → 控制台打印 `🧍 站起来活动一下`
3. 浏览器打开 bilibili → 5 秒后变娱乐模式 → 控制台约 2 秒后打印 `⏰ 已经过去 2 分钟了`
4. UI 上的「已持续」和「下次提醒」数字实时跳动
5. 切换模式 → 计时器立即重置，从 0 开始

---

### ✅ 第七步：通知与 UI 控制面板 【已完成】

**目标**：系统托盘通知 + 倍速滑块 + 托盘图标随模式变色 + 关闭窗口最小化到托盘。

---

#### 7.1 创建 `Notifier` — 系统托盘封装

**产出文件**：`notify/Notifier.kt`

**具体逻辑**：
- 初始化 AWT `SystemTray` + `TrayIcon`
- `show(message: String)` → `trayIcon.displayMessage("Flow", message, TrayIcon.MessageType.INFO)`
- `updateIcon(mode: Mode)` → 切换托盘图标颜色（绿色 WORK / 红色 ENTERTAINMENT）
- 图标用程序生成的 16x16 BufferedImage（纯色圆），避免外部资源文件依赖
- 右键菜单：「显示窗口」「退出」

**验证**：编译通过，启动后托盘出现图标

---

#### 7.2 集成 Notifier 替代控制台 println

**修改文件**：`main.kt`、`notify/Notifier.kt`

**具体变更**：
- `main.kt` 中创建 `Notifier` 实例
- `ReminderEngine` 的 `onReminder` 回调改为调用 `notifier.show(rule.message)`
- 模式变化时调用 `notifier.updateIcon(mode)`

**验证**：提醒触发时托盘弹出气泡消息（而非仅控制台打印）

---

#### 7.3 添加时间倍速滑块

**修改文件**：`ui/App.kt`

**具体变更**：
- 在 UI 底部新增一个 `Slider` 组件：1x ~ 120x，默认 60x
- 拖拽时调用 `reminderEngine.updateTimeScale(newScale)`
- 旁边显示当前倍速文字 `"${scale}x"`

**验证**：拖动滑块到 120x → 倒计时速度明显加快（2 秒变 1 秒）

---

#### 7.4 关闭窗口最小化到托盘

**修改文件**：`main.kt`

**具体变更**：
- `Window.onCloseRequest` 改为 `window.isVisible = false`（隐藏而非退出）
- 托盘右键菜单「显示窗口」→ `window.isVisible = true`
- 托盘右键菜单「退出」→ `exitApplication()`

**验证**：点窗口 X → 窗口消失但进程不退出 → 托盘右键「显示窗口」→ 窗口恢复

---

**验收标准（手动操作）**：
1. 启动后托盘出现绿色圆点图标 ✅
2. @60x 约 15 秒后桌面右下角弹出气泡「💧 该喝水了」
3. 打开 B 站 → 托盘图标变红色 → 约 2 秒后弹出「⏰ 已经过去 2 分钟了」
4. 拖动倍速滑块到 120x → 提醒频率加倍
5. 点 X → 窗口隐藏到托盘 → 右键托盘图标 → 显示窗口 / 退出

---

### ✅ 第八步：集成联调 【已完成】

**目标**：端到端场景测试，确保所有模块协同工作，产出测试报告。

> 本步骤不写新代码，只做手工验证和记录。

---

#### 8.1 准备环境

**操作**：
1. 关闭所有 Java 进程：`taskkill /f /im java.exe`
2. 清理 Gradle 缓存：`./gradlew --stop`
3. 确保 Chrome 扩展已加载（chrome://extensions → Flow Monitor 已启用）
4. 启动应用：`./gradlew :desktopApp:run`

**验证**：浏览器扩展 Service Worker Console 显示 `✅ WebSocket 已连接`

---

#### 8.2 功能测试矩阵

请逐个执行以下场景，记录√或✗：

| # | 场景 | 操作 | 预期结果 | 结果 |
|---|------|------|---------|------|
| T1 | 窗口监测-桌面 | 切换不同应用窗口 | UI 桌面窗口卡片实时更新标题和进程名 | ✅ |
| T2 | 窗口监测-浏览器 | Chrome 切换标签页 | UI 浏览器标签页卡片实时更新域名和标题 | ✅ |
| T3 | 模式切换-进入娱乐 | 浏览器打开 bilibili.com，等 5 秒 | 模式指示条变红「娱乐模式」，托盘图标变红 | ✅ |
| T4 | 模式切换-恢复工作 | 关闭 bilibili，切回 IDE，等 5 秒 | 模式指示条变绿「工作模式」，托盘图标变绿 | ✅ |
| T5 | 工作提醒-喝水 | @60x 启动后等待 ~15 秒 | 托盘弹出气泡「💧 该喝水了」 | ✅ |
| T6 | 工作提醒-站立 | 继续等待 ~25 秒（总计 40 秒） | 托盘弹出气泡「🧍 站起来活动一下」 | ✅ |
| T7 | 娱乐提醒-时间流逝 | 进入娱乐模式后等待 ~2 秒 | 托盘弹出气泡「⏰ 已经过去 2 分钟了」，持续每 2 秒弹出 | ✅ |
| T8 | 计时状态 | 观察 UI「⏱ 计时状态」卡片 | 「已持续」数字递增，「下次提醒」倒计时递减 | ✅ |
| T9 | 倍速滑块 | 拖动滑块到 120x | 倒计时速度明显加快，提醒间隔缩短 | ✅ |
| T10 | 手动触发 | 点击「手动触发提醒」按钮 | 立即弹出当前模式的第一条提醒 | ✅ |
| T11 | 关闭到托盘 | 点窗口 X | 窗口消失，托盘图标仍在，进程未退出 | ✅ |
| T12 | 托盘恢复 | 右键托盘图标 →「显示窗口」 | 窗口恢复显示 | ✅ |
| T13 | 托盘退出 | 右键托盘图标 →「退出」 | 进程完全退出 | ✅ |
| T14 | 扩展降级 | 关闭 Chrome 扩展 | TabServer 打印「浏览器已断开」，桌面窗口监测仍正常 | ✅ |

---

#### 8.3 已知问题确认

| # | 问题 | 现象 | 状态 |
|---|------|------|------|
| I1 | 控制台乱码 | Gradle 日志中文显示为乱码 | 🟡 已知，待后续修复 |
| I2 | 托盘菜单方块 | 右键菜单中文显示为方块 | 🟡 已知，待后续修复 |

---

**验收通过标准**：T1~T14 全部 √，I1~I2 已记录。

---

## 第八步之后：打包与发布（已完成）

### 🔧 Windows MSI 打包

**产出**：`desktopApp/build/compose/binaries/main/msi/Flow-0.1.0.msi` (64 MB)

**配置**：
- `targetFormats(TargetFormat.Msi)` — 仅 Windows
- `packageName = "Flow"`, `vendor = "Flow"`
- `jvmArgs += "-Dfile.encoding=UTF-8"` — 编码设置

**构建命令**：`./gradlew :desktopApp:packageMsi`

---

### 🔧 扩展自动安装

**问题**：MSI 安装后无窗口，托盘菜单无响应。

**根因**：`ExtensionInstaller` 写入 `C:\Program Files\Flow\extension\` 因权限不足崩溃，导致 Compose 组合中断，Window 未创建。

**修复**：
- 目标目录改为 `%USERPROFILE%\.flow\extension\`（始终可写）
- `ensureInstalled()` 从同步 `remember{}` 改为异步 `LaunchedEffect`
- 文件写入加 `try-catch`，失败不崩溃

**产出**：`setup/ExtensionInstaller.kt`

---

### 🔧 扩展安装引导优化

**问题**：已装扩展的用户每次启动都会闪过橙色引导卡片（~200ms）。

**修复**：3 秒延迟逻辑——3 秒内扩展连上则不显示引导，超时才显示。

**改动**：`ui/App.kt` 中 `showSetupGuide` 状态 + `delay(3000)` 判断

---

### 🔧 已知编码/字体问题

| # | 问题 | 原因 | 状态 |
|---|------|------|------|
| I1 | 控制台中文乱码 | JVM file.encoding 与终端编码不匹配 | 🟡 已尝试修复，未生效 |
| I2 | 托盘菜单中文方块 | AWT 系统托盘使用原生菜单，setFont 可能无效 | 🟡 已尝试修复，未生效 |

详细记录见 `ISSUES.md`。
