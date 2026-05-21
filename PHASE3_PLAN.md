# Flow - 第三阶段开发计划

## 需求分析

### 优先级排序（重要性 × 吸引力 ÷ 难度）

| 优先级 | # | 需求 | 重要性 | 吸引力 | 难度 | 说明 |
|--------|---|------|--------|--------|------|------|
| **P0** | #3 | 默认窗口尺寸优化 | ⭐⭐⭐ | ⭐⭐ | 🔧 | 500×640 太窄长，改为正常横版比例 |
| **P0** | #1 | 首页可滚动 | ⭐⭐⭐ | ⭐ | 🔧 | 窗口小时内容被截断，用户看不到完整信息 |
| **P1** | #2 | Tab 式导航（首页 / 设置） | ⭐⭐ | ⭐⭐ | 🔧🔧 | 首页和设置混在一起不合理，应分 Tab |
| **P1** | #4 | 白名单功能（频道级） | ⭐⭐⭐ | ⭐⭐ | 🔧🔧 | 娱乐站的学习频道应识别为工作模式 |
| **P2** | #5 | 自定义表情包弹窗 | ⭐⭐ | ⭐⭐⭐ | 🔧🔧🔧 | 透明背景 + 随机表情 + 文字，像微信表情 |

---

## 分阶段实施

### Phase 3.1：基础体验修复（P0）

#### ✅ 3.1.1 默认窗口尺寸优化 【已完成】

**修改**：`main.kt` 窗口从 500×640 → 680×500（横版比例）

---

#### ✅ 3.1.2 首页可滚动 【已完成】

**修改**：`App.kt` 外层 Column 添加 `verticalScroll(rememberScrollState())`

---

### ✅ Phase 3.2：UI 重构（P1）【已完成】

| 子任务 | 状态 |
|--------|------|
| 3.2.1 Tab 导航 | ✅ |

#### 3.2.1 Tab 式导航

**目标**：首页和设置分成两个 Tab，各自独立显示。

**子步骤**：

##### 步骤 A：创建 Tab 状态 + TabRow UI

**修改文件**：`ui/App.kt`

**操作**：
- 新增 `var selectedTab by remember { mutableStateOf(0) }`（0=Dashboard, 1=Settings）
- 在标题下方添加 `TabRow`：两个 Tab 项「Dashboard」「Settings」
- `TabRow` 使用 Material3 组件

**验证**：编译通过，UI 出现两个 Tab 标签

---

##### 步骤 B：提取 Dashboard 为独立 Composable

**新建文件**：`ui/DashboardPanel.kt`

**操作**：
- 将 App.kt 中现有的模式条、计时状态、桌面窗口、浏览器标签页、扩展引导、倍速滑块、手动触发按钮全部移到 `DashboardPanel`
- `DashboardPanel` 接收参数：`activeWindow`, `browserMessage`, `currentMode`, `elapsedVirtualMs`, `nextReminderVirtualMs`, `timeScale`, `developerMode`, `showSetupGuide`, `extensionDir`, 以及回调函数
- 顶部标题 "Flow" + 7连击逻辑保留在 App.kt（Tab 切换不清除状态）

**验证**：编译通过，Dashboard 内容完整

---

##### 步骤 C：Tab 切换显示对应面板

**修改文件**：`ui/App.kt`

**操作**：
- 删除原嵌入的 SettingsPanel 和所有 Dashboard 内容
- 改为 `when(selectedTab) { 0 -> DashboardPanel(...); 1 -> SettingsPanel(...) }`
- TabRow 放在标题下方、内容上方

**验证**：
1. 启动默认显示 Dashboard Tab
2. 点击 Settings Tab → 切换到黑名单编辑
3. 点回 Dashboard Tab → 切换回首页

---

##### 步骤 D：每个 Tab 内容独立可滚动

**修改文件**：`ui/DashboardPanel.kt`、`ui/SettingsPanel.kt`

**操作**：
- DashboardPanel 的 Column 添加 `verticalScroll`
- SettingsPanel 已有 `verticalScroll`，确认正常

**验证**：两个 Tab 各自可独立滚动，互不影响

---

### 🔜 Phase 3.3 修复：按 Spec 重写分类逻辑 【进行中】

**问题**：merge 两个数据流导致窗口轮询打断扩展分类的 streak。

**根据**：`CLASSIFIER_SPEC.md`

**子步骤**：

##### 步骤 1：在 App.kt 中新增两个追踪变量

**操作**：
- 新增 `var latestBrowserMessage: BrowserMessage? = null`（普通变量，非 Compose state）
- 复用已有 `var extensionConnected = false`（已在代码中）

**目标**：变量声明完毕，编译通过
**验收**：`./gradlew :desktopApp:compileKotlin` 成功

---

##### 步骤 2：TabServer 事件更新这两个变量

**操作**：
- `tabServer.messages` 的 `collectLatest` 中，增加 `latestBrowserMessage = msg`
- **新增**：监听 TabServer 连接/断开状态。TabServer 的 `onOpen` / `onClose` 通过回调通知 App.kt
- 在 `TabServer` 中新增两个回调参数：`onConnected: () -> Unit`, `onDisconnected: () -> Unit`

**目标**：扩展连接/断开时 `extensionConnected` 准确变化；每次收到消息时 `latestBrowserMessage` 更新
**验收**：启动应用，Chrome 扩展连接 → 终端日志确认变量变化

---

##### 步骤 3：删除旧 merge 逻辑，改为单入口分类

**操作**：
- 删除以下代码：
  ```kotlin
  val windowResults = ... .map { classifyWindow(it) }
  val browserResults = ... .map { classifyBrowser(it) }
  val merged = merge(windowResults, browserResults)
  modeDetector.detect(merged).collect { ... }
  ```
- 替换为：
  ```kotlin
  WindowMonitor.observeActiveWindow().collect { window ->
      val result = classifySingle(window)
      modeDetector.detect(flowOf(result)).collect { ... }
  }
  ```

**目标**：每次 ActiveWindow 到达时，执行一次判定，产生一个 ClassificationResult，送入 Detector
**验收**：编译通过

---

##### 步骤 4：实现 `classifySingle()` 函数

**操作**：
- 在 App.kt 中创建私有函数 `classifySingle(window: ActiveWindow): ClassificationResult`
- 逻辑按 Spec Step1-3：
  1. 判断 `processName` 是否是浏览器
  2. 是浏览器 + 扩展已连接 + latestBrowserMessage 存在 → 用 `classifyBrowser(latestBrowserMessage)`
  3. 否则 → 用 `classifyWindow(window)`
- 返回 ClassificationResult

**目标**：每次调用返回唯一的 ClassificationResult
**验收**：编译通过

---

##### 步骤 5：修复 ModeDetector 集成

**操作**：
- 因为旧代码用 `modeDetector.detect(merged)` 接收 Flow，新代码每次产生一个 result
- 改为：用一个 `MutableSharedFlow<ClassificationResult>` 作为 Detector 的输入流
- `classifySingle()` 的结果 emit 到这个 SharedFlow
- Detector 从 SharedFlow 读取

**目标**：单次分类结果正确送入 Detector 做 5 秒防抖
**验收**：编译通过

---

##### 步骤 6：端到端场景验证

**场景 A：应用→浏览器（扩展在线）**
- 操作：当前窗口是 VS Code，然后切换到 Chrome(YouTube)
- 预期：日志出现 `[Classifier] BROWSER ... ENTERTAINMENT`，5 秒后 `[Detector] SWITCH -> ENTERTAINMENT`
- 操作：切回 VS Code
- 预期：5 秒后切回 WORK

**场景 B：浏览器（扩展离线）**
- 操作：关闭扩展，前台窗口切换到 Chrome(YouTube)
- 预期：日志出现 `[Classifier] WINDOW ...`，用窗口标题分类

**场景 C：桌面应用**
- 操作：切换到 Steam
- 预期：日志出现 `[Classifier] WINDOW ... HIT 'steam' -> ENTERTAINMENT`

#### 3.3.1 URL 路径白名单

**目标**：娱乐站的特定学习频道强制识别为工作模式。

**优先级规则**：白名单 > 黑名单

**子步骤**：

##### 步骤 A：AppConfig 增加 whitelistUrls 字段

**修改文件**：`classify/ConfigManager.kt`

**操作**：
- `AppConfig` 新增 `val whitelistUrls: List<String> = listOf()`
- 预置示例注释（不强制填入，用户自行添加）

**验证**：编译通过，config.json 自动包含 `whitelistUrls: []`

---

##### 步骤 B：ModeClassifier 白名单优先匹配

**修改文件**：`classify/ModeClassifier.kt`

**操作**：
- `classifyBrowser()` 逻辑改为：
  1. 先检查 URL 是否命中 whitelistUrls（`msg.url.contains(pattern, ignoreCase = true)`）→ WORK
  2. 再检查 domain 是否命中 entertainmentDomains → ENTERTAINMENT
  3. 都不命中 → WORK

**验证**：编译通过

---

##### 步骤 C：SettingsPanel 新增白名单编辑区

**修改文件**：`ui/SettingsPanel.kt`

**操作**：
- 在域名列表上方新增「Whitelist URLs」区域
- 列表 + 输入框 + 添加/删除按钮（与域名、应用相同的模式）
- placeholder 示例：`youtube.com/@freecodecamp`
- 自动保存（与域名、应用一致的逻辑）

**验证**：编译通过，Settings Tab 出现白名单编辑区

---

##### 步骤 D：端到端测试

**验证**：
| 操作 | 预期 |
|------|------|
| 添加白名单 `youtube.com/@freecodecamp` | 访问该频道 → 工作模式（绿色） |
| 访问其他 YouTube 视频 | 仍是娱乐模式（红色） |
| 删除白名单项 | 该频道恢复娱乐模式 |
| 关闭重开 | 白名单项持久保留 |

---

### Phase 3.4：表情包弹窗（P2）

#### 3.4.1 自定义表情包通知

**问题**：当前通知弹窗是深灰实心卡片，不够有趣。

**方案**：
- 用户指定一个表情包文件夹路径（存在 config.json）
- 每次提醒触发时，随机选取文件夹中的一张图片
- 弹窗改为透明背景，只显示表情包 + 文字
- 位置：右下角，比当前稍大
- 生命周期：弹出 → 停留 3 秒 → 缩小消失

**技术要点**：
- 透明窗口已在 NotificationPopup 实现（`transparent = true`）
- 图片加载：`ImageIO.read()` + Skia 转 BitmapPainter
- 随机选择：`folder.listFiles()?.filter { it.endsWith(".png") || ... }?.random()`

**产出**：
- 更新 `notify/NotificationPopup.kt` — 支持图片模式
- 更新 `classify/ConfigManager.kt` — 加 `emojiFolder` 字段
- 更新 `ui/SettingsPanel.kt` — 加文件夹选择

**验证**：
| 操作 | 预期 |
|------|------|
| 设置表情包文件夹 | 下次提醒时弹窗含随机表情 |
| 不设置文件夹 | 回退到纯文字模式（当前行为） |

---

## 实施顺序

```
Phase 3.1 (P0) → 窗口尺寸 + 首页滚动
Phase 3.2 (P1) → Tab 导航重构
Phase 3.3 (P1) → 白名单功能
Phase 3.4 (P2) → 表情包弹窗
```
