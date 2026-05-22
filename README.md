# Flow

专注力管理桌面应用 — 自动监测你的工作/娱乐模式，智能提醒你保持专注。

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blueviolet)
![Compose](https://img.shields.io/badge/Compose%20Multiplatform-1.7.1-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20Desktop-lightgrey)

## 功能

- **自动模式识别**：监测当前窗口标题 + 浏览器标签页 URL，自动判断你在工作还是娱乐
- **智能提醒**：工作模式定时提醒喝水/站立，娱乐模式提醒控制时间
- **可配置黑/白名单**：自定义娱乐域名、应用关键词、学习频道白名单
- **表情包弹窗**：提醒时随机展示自定义表情包图片（透明背景）
- **音效提醒**：提醒时随机播放自定义音效（WAV）
- **时间加速**：演示/测试用倍速模式，默认 60x（1 秒 = 1 分钟）
- **系统托盘**：最小化到托盘，后台持续运行

## 技术栈

| 层 | 技术 |
|----|------|
| 语言 | Kotlin 2.0.21 |
| UI 框架 | Compose Multiplatform (Desktop) |
| 构建工具 | Gradle + Kotlin DSL |
| 窗口监测 | JNA (Java Native Access) — Win32 API |
| 浏览器扩展 | Chrome Extension Manifest V3 + WebSocket |
| 通信协议 | WebSocket (localhost:9527) |
| 序列化 | kotlinx.serialization |
| 音频播放 | javax.sound.sampled (JDK 内置) |

## 项目结构

```
├── desktopApp/                  # 桌面应用主模块
│   └── src/main/kotlin/org/example/flow/
│       ├── main.kt              # 入口 + 窗口创建 + 集成
│       ├── model/               # 数据模型 (Mode, ActiveWindow, BrowserMessage)
│       ├── monitor/             # 窗口监测 (JNA Win32 API)
│       ├── classify/            # 模式分类 + 配置管理
│       ├── engine/              # 提醒引擎 (定时器 + 加速)
│       ├── server/              # WebSocket 服务端
│       ├── notify/              # 通知弹窗 + 表情包 + 音效
│       ├── setup/               # 扩展安装 + 图标生成
│       └── ui/                  # Compose UI (Dashboard, Settings)
├── extension/                   # Chrome 浏览器扩展
│   ├── manifest.json
│   └── background.js
├── shared/                      # 共享代码 (预留多平台)
└── build.gradle.kts             # 根构建脚本
```

## 运行方式

### 环境要求

- JDK 17+
- Gradle 8.x（项目自带 Gradle Wrapper）
- Windows 10/11
- Chrome / Edge 浏览器（用于扩展）

### 1. 启动桌面应用

```bash
./gradlew :desktopApp:run
```

### 2. 安装浏览器扩展

首次启动后，应用会自动将扩展安装到 `%USERPROFILE%/.flow/extension/`。

在 Chrome 中：
1. 打开 `chrome://extensions/`
2. 开启「开发者模式」
3. 点击「加载已解压的扩展」→ 选择 `%USERPROFILE%/.flow/extension/` 文件夹

### 3. 配置（可选）

配置文件自动生成在 `%USERPROFILE%/.flow/config.json`，也可通过应用内 Settings Tab 编辑：

- 娱乐域名黑名单
- 娱乐应用关键词
- URL 白名单（学习频道）
- 表情包文件夹路径
- 音效文件夹路径

### 4. 添加表情包（可选）

将 JPG / PNG / JFIF 图片放入 `%USERPROFILE%/.flow/emojis/`，提醒时随机展示。

### 5. 添加音效（可选）

将 WAV 音频文件放入 `%USERPROFILE%/.flow/sounds/`，提醒时随机播放。

## 开发命令

```bash
# 编译
./gradlew :desktopApp:compileKotlin

# 运行
./gradlew :desktopApp:run

# 打包 MSI 安装包
./gradlew :desktopApp:createDistributable
```

## 许可证

MIT
