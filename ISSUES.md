# Flow - 问题追踪

## Issue #1: Gradle 控制台日志中文乱码

**状态**：🟡 已知问题（待后续修复）
**发现版本**：第七步完成后
**现象**：运行 `./gradlew :desktopApp:run` 后，终端输出的中文日志显示为乱码。

**链路分析**：

```
Kotlin 源码 (UTF-8)
  → JVM 内存 (UTF-16)
  → System.out.println()
  → Java 进程 stdout
  → Gradle 转发
  → Windows 终端渲染
```

| 环节 | 检查点 | 状态 |
|------|--------|------|
| 源码编码 | `println` 中的中文字符串 | ✅ UTF-8 正确存储 |
| JVM 内部编码 | `String` 在 JVM 中永远是 UTF-16 | ✅ 无问题 |
| System.out 输出编码 | JVM `file.encoding` 默认值 | ❌ Windows 中文版默认 GBK |
| Gradle 转发 | Gradle 透传 stdout | ✅ 不做编码转换 |
| 终端渲染 | Windows 终端代码页 | 可能是 GBK(936) 或 UTF-8(65001) |

**根因**：JVM 的 `file.encoding` 默认为系统编码 GBK，与 Gradle 输出的 UTF-8 不一致。

**尝试的修复**：添加 `-Dfile.encoding=UTF-8` 到 `gradle.properties` 和 `build.gradle.kts`，但未生效。

**可能原因**：
1. Gradle 配置缓存可能未刷新 → 需清理缓存后重试
2. Windows 终端自身代码页为 GBK → 需在运行前执行 `chcp 65001`
3. Gradle Daemon 未重启 → 需 `./gradlew --stop` 后重试

---

## Issue #2: 托盘右键菜单中文显示为方块

**状态**：🟡 已知问题（待后续修复）
**发现版本**：第七步完成后
**现象**：系统托盘右键菜单的「显示窗口」和「退出」显示为方块（□□）。

**链路分析**：

```
Notifier.kt
  → MenuItem("显示窗口") 创建 AWT MenuItem
  → AWT 使用系统默认 GUI 字体渲染
  → Windows 原生菜单控件渲染
  → 屏幕显示
```

| 环节 | 检查点 | 状态 |
|------|--------|------|
| 字符串内容 | Kotlin 中 `"显示窗口"` | ✅ 源码 UTF-8，运行时可正确读取 |
| AWT MenuItem 创建 | `MenuItem(String)` 构造 | ✅ 创建成功 |
| AWT 字体设置 | `MenuItem.setFont()` | ⚠️ 系统托盘使用原生菜单，可能不遵循 AWT 字体设置 |
| 系统字体可用性 | 系统是否安装了中文字体 | ✅ Windows 通常有 Microsoft YaHei |

**根因**：Windows 系统托盘菜单是原生控件，AWT 只做了薄封装。`MenuItem.setFont()` 对系统托盘菜单可能无效，菜单渲染由 Windows 自身的字体回退机制决定。

**尝试的修复**：设置 `Font("Microsoft YaHei", Font.PLAIN, 12)`，但未生效。

**可能原因**：
1. `Microsoft YaHei` 字体名在 AWT 中的映射名可能不同
2. Windows 系统托盘的原生菜单控件不受 AWT 字体设置影响
3. 需检查 `GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()` 获取可用字体名
