# Flow - 分类逻辑规格说明书

## 核心原则

```
判定当前模式时，数据源互斥，不同时参与：
「当前前台窗口是浏览器 + 扩展已连接」→ 只用扩展数据
「其他所有情况」→ 只用窗口标题数据
```

---

## 数据结构

### ActiveWindow（窗口轮询产出，每 500ms）
```
title: String        // 窗口标题，如 "YouTube - Google Chrome"
processName: String  // 进程名，如 "chrome.exe"
timestamp: Long
```

### BrowserMessage（扩展产出，标签页变化时）
```
url: String          // 如 "https://www.youtube.com/watch?v=abc"
domain: String       // 如 "www.youtube.com"
title: String        // 如 "[4K] Tutorial - YouTube"
```

---

## 判定步骤（单次原子操作）

每次 `ActiveWindow` 到达时，执行以下**有且仅有一轮**判定：

### Step 1：识别窗口类型

```
读取 processName，转小写

是否为浏览器进程？
  chrome.exe   → 是
  msedge.exe   → 是
  firefox.exe  → 是
  brave.exe    → 是
  opera.exe    → 是
  其他         → 否
```

### Step 2：根据类型选择数据源

```
IF 窗口是浏览器进程
    IF 扩展已连接（extensionConnected = true）
        IF 扩展发过消息（latestBrowserMessage != null）
            → 数据源 = latestBrowserMessage
        ELSE
            → 数据源 = none，分类结果 = WORK
    ELSE（扩展未连接）
        → 数据源 = ActiveWindow.title
ELSE（窗口不是浏览器）
    → 数据源 = ActiveWindow.title
```

### Step 3：对数据源执行分类

```
IF 数据源 is BrowserMessage:
    Step 3a: 白名单检查
        url 包含 whitelistUrls 中任一模式？
        YES → 结果 = WORK, 关键词 = "whitelist:xxx"

    Step 3b: 黑名单检查
        domain 包含 entertainmentDomains 中任一域名？
        YES → 结果 = ENTERTAINMENT, 关键词 = "youtube.com"

    Step 3c: 默认
        结果 = WORK, 关键词 = null

IF 数据源 is ActiveWindow.title:
    title 包含 entertainmentApps 中任一关键词？
    YES → 结果 = ENTERTAINMENT, 关键词 = "steam"
    NO  → 结果 = WORK, 关键词 = null

IF 数据源 is none:
    结果 = WORK, 关键词 = null
```

### Step 4：送入 ModeDetector（5 秒防抖）

```
本次分类结果 → ModeDetector.detect()
    → 连续同结果 5 秒 → 切换模式
    → 中途出现异结果 → 重置计时
```

---

## 扩展状态管理

### 扩展已连接（extensionConnected）
```
初始化 = false
TabServer.onOpen()    → 设为 true
TabServer.onClose()   → 设为 false
收到 BrowserMessage   → 也是"已连接"的信号，设为 true
```

### 最新浏览器消息（latestBrowserMessage）
```
初始化 = null
TabServer 收到 BrowserMessage → 更新 latestBrowserMessage
扩展断开时 → 不清空（窗口类型切换时自然不再使用）
```

---

## 完整判定时间线示例

### 场景：用户从 VS Code 切换到 Chrome(YouTube)，扩展已连接

```
T=0ms    ActiveWindow(title="Flow.kt - IntelliJ IDEA", process="idea64.exe")
           → 不是浏览器 → 用 title → 未命中黑名单 → WORK
           → Detector: WORK (无变化)

T=500ms  ActiveWindow(title="YouTube - Google Chrome", process="chrome.exe")
           → 是浏览器 + 扩展已连接 + latestBrowserMessage 存在
           → 用 latestBrowserMessage(url="youtube.com", domain="www.youtube.com")
           → 黑名单命中 "youtube.com" → ENTERTAINMENT
           → Detector: Streak start ENTERTAINMENT

T=1000ms ActiveWindow(title="YouTube - Google Chrome", process="chrome.exe")
           → 同上 → ENTERTAINMENT

T=1500ms ... (每 500ms 重复，持续同结果)

T=5000ms Detector: 连续 5000ms → SWITCH to ENTERTAINMENT ✅

T=5500ms 用户切回 VS Code
           ActiveWindow(title="Flow.kt - IntelliJ IDEA", process="idea64.exe")
           → 不是浏览器 → 用 title → 未命中黑名单 → WORK
           → Detector: Streak start WORK

T=10500ms Detector: 连续 5000ms → SWITCH to WORK ✅
```

---

## 实现要点

1. **不再 merge 两个流**。只保留一个分类入口：每次 `ActiveWindow` 到达时判定。
2. `latestBrowserMessage` 是一个普通变量（非 Compose state），每次判定时读取。
3. `extensionConnected` 在 TabServer 事件中更新。
4. 日志格式保持现有的 `[Classifier]` / `[Detector]` 前缀。
