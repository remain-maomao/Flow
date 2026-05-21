# Flow - 开发进度

## 今日完成（2026-05-21）

### Phase 1（MVP）
- [x] 项目骨架 + Gradle 依赖（JNA, WebSocket, Serialization）
- [x] 窗口监测（JNA: GetForegroundWindow + GetWindowText）
- [x] Chrome 浏览器扩展（Manifest V3 + WebSocket）
- [x] WebSocket 服务端（localhost:9527）
- [x] 模式分类器（娱乐黑名单 + 5 秒防抖）
- [x] 提醒引擎（工作 15min/40min，娱乐 2min，60x 演示加速）
- [x] 系统托盘 + 通知弹窗 + 倍速滑块
- [x] 集成联调（14 项测试全部通过）

### Phase 2（P0-P3 提升）
- [x] 2.1.1 托盘菜单乱码 → 英文降级（框架 Bug CMP-4486）
- [x] 2.1.2 自定义通知弹窗（深灰圆角卡片 + 淡入动画）
- [x] 2.2.1 娱乐黑名单可配置（config.json + SettingsPanel + 自动保存）
- [x] 2.2.2 设置面板可滚动（verticalScroll）
- [x] 2.3.1 应用图标统一（painterResource + 用户自定义图标）
- [x] 2.3.2 隐藏演示模式（标题 7 连击解锁）

### Phase 3（5 项改进）
- [x] 3.1.1 窗口尺寸优化（500×640 → 680×500）
- [x] 3.1.2 首页可滚动
- [x] 3.2 Tab 导航（Dashboard / Settings）
- [x] 3.3 白名单 + 分类重写
  - [x] URL 白名单（学习频道强制 WORK）
  - [x] 分类 Bug 修复（merge 改为互斥选择）
  - [x] State 捕获陷阱修复（extensionConnected: Boolean → State<Boolean>）
- [x] 3.4 表情包弹窗（随机图片 + 透明背景）

### 文档
- [x] PLAN.md — 实施计划
- [x] ISSUES.md — 问题追踪
- [x] CLASSIFIER_SPEC.md — 分类逻辑规格
- [x] POSTMORTEM.md — Bug 复盘
- [x] PHASE2_PLAN.md — 第二阶段计划
- [x] PHASE3_PLAN.md — 第三阶段计划

### 已知问题
| # | 问题 | 状态 |
|---|------|------|
| I1 | Gradle 终端中文乱码 | 🟡 JVM/Gradle/Windows 编码桥接，非应用层可修 |
| I2 | 托盘菜单中文方块 | 🟡 Compose 框架 Bug (CMP-4486)，已英文降级 |

---

## 项目结构

```
desktopApp/src/main/kotlin/org/example/flow/
├── main.kt
├── model/
│   ├── ActiveWindow.kt
│   ├── BrowserMessage.kt
│   ├── ClassificationResult.kt
│   └── Mode.kt
├── monitor/
│   ├── Win32.kt
│   └── WindowMonitor.kt
├── classify/
│   ├── ConfigManager.kt
│   ├── ModeClassifier.kt
│   └── ModeDetector.kt
├── engine/
│   ├── ReminderRule.kt
│   └── ReminderEngine.kt
├── server/
│   └── TabServer.kt
├── notify/
│   ├── Notifier.kt
│   ├── NotificationPopup.kt
│   └── EmojiPicker.kt
├── setup/
│   ├── ExtensionInstaller.kt
│   └── IconGenerator.kt
└── ui/
    ├── App.kt
    ├── DashboardPanel.kt
    └── SettingsPanel.kt

extension/
├── manifest.json
└── background.js

用户目录 ~/.flow/
├── config.json
├── extension/
└── emojis/
```

## 下一步

Phase 3 全部完成。等待用户提出新需求。
