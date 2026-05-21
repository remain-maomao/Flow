# Flow - 第二阶段开发计划

## 问题清单（按优先级排序）

| 优先级 | # | 问题 | 重要性 | 难度 | 说明 |
|--------|---|------|--------|------|------|
| **P0** | I2 | 托盘右键菜单中文乱码 | ⭐⭐⭐ 核心 UX | 🔧🔧 中 | 右键菜单「显示窗口」「退出」显示为方块，用户无法正常操作 |
| **P0** | #5 | 提醒弹窗太丑 | ⭐⭐⭐ 核心 UX | 🔧🔧 中 | Windows 原生气泡太简陋（大感叹号），应改为自定义 Compose 弹窗 |
| **P1** | #4 | 娱乐黑名单可配置 | ⭐⭐ 可用性 | 🔧 低 | 目前硬编码 17 个域名 + 13 个标题关键词，用户无法自定义 |
| **P2** | #1 | 应用图标不统一 | ⭐ 体验打磨 | 🔧 低 | 任务栏图标 vs 托盘图标风格不一致，应用无专属图标 |
| **P3** | #3 | 演示模式隐藏 | ⭐ 开发体验 | 🔧 低 | 倍速滑块对普通用户无意义，应像 Android 一样通过「点击版本号 7 次」激活 |

---

## 分阶段实施

### 🔜 Phase 2.1：核心 UX 修复（P0）【进行中】

---

#### ✅ 2.1.1 托盘菜单乱码修复 【已完成】

**目标**：托盘右键菜单正常显示文字。

**子步骤**：

##### 步骤 A：诊断 — 列出系统所有可用 AWT 字体

**操作**：
- 在 `main.kt` 启动时打印 `GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames` 的前 30 个字体名
- 手动从中找出包含 "YaHei" / "SimSun" / "Song" / "Hei" 等关键词的字体
- 记录正确的字体名（AWT 中的名字可能与 Windows 显示名不同）

**验证**：终端输出字体列表，确认存在中文字体

---

##### 步骤 B：应用正确字体

**操作**：
- 用步骤 A 找到的正确字体名替换 `Notifier.kt` 中的 `Font("Microsoft YaHei", ...)`
- 对 MenuItem 和 PopupMenu 都设置字体

**验证**：编译通过，安装 MSI 后托盘菜单显示中文

---

##### 步骤 C：英文降级方案（如果步骤 B 仍失败）

**操作**：
- 将 MenuItem 文案从「显示窗口」「退出」改为 `"Show"` / `"Exit"`
- 如果 AWT 字体设置对原生托盘菜单确实无效，至少英文 ASCII 在所有字体中都能正常渲染

**验证**：托盘菜单显示 "Show" / "Exit"，英文正常渲染

---

**最终验收**：✅ 右键托盘图标 → 菜单显示 "Show Window" / "Exit"，英文正常渲染

**根因确认**：Compose Multiplatform 框架 Bug（[GitHub #4486](https://github.com/JetBrains/compose-multiplatform/issues/4486)），等待框架修复后改回中文。

---

#### 2.1.2 自定义提醒弹窗

**目标**：用 Compose 自定义弹窗替代 Windows 原生 `TrayIcon.displayMessage()`。

**子步骤**：

##### 步骤 A：创建 `NotificationPopup` Composable

**产出文件**：`notify/NotificationPopup.kt`

**具体逻辑**：
- 一个独立 `Window`（非主窗口），`undecorated = true`, `alwaysOnTop = true`, `transparent = true`
- 定位：屏幕右下角，偏移 (20, 40) 像素
- 内容：圆角卡片 (12dp) + 左侧 4px 粗色条（绿/红） + emoji + 文案
- 尺寸：宽 300dp，高自适应
- 通过 `rememberWindowState(position = ...)` 设置位置

**具体代码骨架**：
```kotlin
@Composable
fun NotificationPopup(
    message: String,
    mode: Mode,
    onDismiss: () -> Unit,
) {
    val screenBounds = GraphicsEnvironment.localGraphicsEnvironment
        .defaultScreenDevice.defaultConfiguration.bounds
    val windowState = rememberWindowState(
        position = WindowPosition(
            WindowPosition.Platform.Aligned.TOP_RIGHT,  // 用偏移模拟右下
        ),
        width = 300.dp,
    )
    // 或者用绝对坐标：
    // x = screenBounds.width - 320.dp, y = screenBounds.height - 120.dp

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        undecorated = true,
        alwaysOnTop = true,
        transparent = true,
        focused = false,
    ) {
        // 圆角卡片 + 色条 + 文字
    }
}
```

**验证**：编译通过

---

##### 步骤 B：添加动画（淡入 + 自动消失）

**操作**：
- 用 `AnimatedVisibility` 包裹卡片内容
- `LaunchedEffect` 内 `delay(4_000)` 后调用 `onDismiss()`
- 淡入动画：`fadeIn(tween(300))`
- 淡出动画：`fadeOut(tween(500))`

**验证**：弹窗出现时有淡入效果，4 秒后自动淡出

---

##### 步骤 C：与 `ReminderEngine` 对接

**修改文件**：`main.kt`、`Notifier.kt`

**操作**：
- `Notifier.show()` 不再调用 `trayIcon.displayMessage()`，改为触发通知弹窗
- `Notifier` 持有 `currentMode` 和 `currentMessage` 状态
- `main.kt` 中新增一个 `NotificationPopup` 实例，由 `Notifier` 控制显示/隐藏
- 状态管理：`notifier` 发射事件 → `main.kt` 中 `LaunchedEffect` 消费 → 显示弹窗

**简化方案**（避免复杂状态传递）：
- `main.kt` 中维护 `var notificationMessage by remember { mutableStateOf<String?>(null) }`
- `ReminderEngine.onReminder` 设为 `{ rule -> notificationMessage = rule.message }`
- 当 `notificationMessage != null` 时渲染 `NotificationPopup`
- `NotificationPopup.onDismiss` 设为 `{ notificationMessage = null }`
- 新提醒直接覆盖旧状态（不堆叠）

**验证**：`./gradlew :desktopApp:run`，@60x 约 15 秒后右下角弹出 Compose 风格通知弹窗（不是 Windows 原生气泡）

---

##### 步骤 D：清理旧代码

**操作**：
- 删除 `Notifier.show()` 中的 `trayIcon.displayMessage()` 调用
- `notifier.show()` 保留但改为通过回调触发通知弹窗状态

**验证**：编译通过，无未使用的 import

---

### Phase 2.2：可用性增强（P1）

#### 2.2.1 娱乐黑名单可配置

**目标**：用户可在 UI 中添加/删除自己的娱乐域名和应用关键词。

**方案**：
- 存储：`%USERPROFILE%\.flow\config.json`
- 数据结构：
  ```json
  {
    "entertainmentDomains": ["bilibili.com", "youtube.com", ...],
    "entertainmentApps": ["steam", "原神", ...]
  }
  ```
- UI：设置面板中显示列表 + 添加/删除按钮
- 首次启动时写入默认值，用户可修改

**验收标准**：
| # | 场景 | 预期 |
|---|------|------|
| 1 | 打开设置 | 显示当前黑名单列表 |
| 2 | 添加 `example.com` | 再次访问时识别为娱乐模式 |
| 3 | 删除 `bilibili.com` | B 站不再触发娱乐模式 |

---

### Phase 2.3：体验打磨（P2-P3）

#### 2.3.1 应用图标统一

**目标**：任务栏和托盘使用统一的应用图标。

**方案**：
- 设计/生成一个 256×256 应用图标（ICO 格式）
- 在 `build.gradle.kts` 中配置 `nativeDistributions.windows.iconFile`
- 托盘图标改为使用同一图标，仅通过颜色滤镜区分模式

---

#### 2.3.2 演示模式隐藏

**目标**：普通用户看不到倍速滑块，开发者通过隐藏操作激活。

**方案**：
- 倍速滑块默认隐藏
- 连续点击版本号/标题文字 7 次 → 弹出 Toast「开发者模式已开启」
- 此时倍速滑块和「手动触发」按钮显示
- 状态持久化到 `config.json`

---

## 文件结构预览（Phase 2 完成后）

```
desktopApp/src/main/kotlin/org/example/flow/
├── main.kt
├── model/          (现有)
├── monitor/        (现有)
├── classify/
│   ├── ModeClassifier.kt
│   ├── ModeDetector.kt
│   └── ConfigManager.kt          ← 新增：读写 config.json
├── engine/         (现有)
├── server/         (现有)
├── notify/
│   ├── Notifier.kt               (现有，简化)
│   └── NotificationPopup.kt      ← 新增：自定义弹窗
├── setup/          (现有)
└── ui/
    ├── App.kt                    (更新)
    ├── SettingsPanel.kt          ← 新增：黑名单配置界面
    └── components/               ← 新增：通用组件
```
