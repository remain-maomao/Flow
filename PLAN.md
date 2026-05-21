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

### 第三步：浏览器扩展开发

**目标**：Chrome 扩展监听标签页变化，通过 WebSocket 发送 URL/标题/域名给桌面端。

**关键任务**：
- Manifest V3 配置
- Service Worker 监听 tabs API
- WebSocket 客户端连接 ws://localhost:9527

**验证方式**：浏览器加载扩展，chrome://extensions 查看日志，切换标签页有输出。

---

### 第四步：WebSocket 服务端

**目标**：桌面端启动 WebSocket Server，接收浏览器扩展消息并解析。

**关键任务**：
- Java-WebSocket Server 启动
- 消息反序列化（JSON → ActiveWindow）
- 与 WindowMonitor 流合并

**验证方式**：打开浏览器扩展后，桌面 UI 显示从扩展收到的 URL。

---

### 第五步：模式分类器

**目标**：根据窗口信息判断工作/娱乐模式（默认工作，娱乐黑名单）。

**关键任务**：
- 娱乐域名黑名单
- 娱乐应用标题黑名单
- 5 秒防抖逻辑

**验证方式**：打开 B 站/YouTube 后 5 秒切换娱乐模式，切回 IDE 后 5 秒恢复工作模式。

---

### 第六步：提醒引擎

**目标**：模式驱动的定时器，支持时间加速。

**关键任务**：
- 模式切换重置计时器
- 工作模式：15min / 40min 提醒
- 娱乐模式：2min 提醒
- 可调时间倍速

**验证方式**：@60x 加速下，娱乐模式 2s 后提醒，工作模式 15s/40s 后提醒。

---

### 第七步：通知与 UI 控制面板

**目标**：系统托盘 + Compose 控制面板 + 通知弹窗。

**关键任务**：
- 系统托盘图标（颜色随模式变化）
- 托盘气泡通知
- 控制面板：模式、时长、倒计时、倍速滑块、手动触发
- 关闭窗口最小化到托盘

**验证方式**：全部 UI 交互正常，托盘通知正常弹出。

---

### 第八步：集成联调

**目标**：端到端场景测试，确保所有模块协同工作。

**测试场景**：
| # | 场景 | 预期 |
|---|------|------|
| 1 | 打开 B 站 | 5s 后娱乐模式，2s 后提醒 |
| 2 | 切回 IDE | 5s 后工作模式，15s 后喝水提醒 |
| 3 | 浏览器扩展识别 YouTube | 域名匹配直接识别 |
| 4 | 关闭浏览器扩展 | 降级到窗口标题解析 |
| 5 | 倍速 120x | 2min 提醒 1s 内触发 |
