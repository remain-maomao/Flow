# Flow - 问题追踪

## Issue #1: Gradle 控制台日志中文乱码

**状态**：✅ 已修复
**发现版本**：第七步完成后
**现象**：运行 `./gradlew :desktopApp:run` 后，终端输出的中文日志（如引擎启动、提醒触发等 println）显示为乱码。

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
| System.out 输出编码 | JVM `file.encoding` 默认值 | ❌ Windows 中文版默认 GBK，与终端 UTF-8 不匹配 |
| Gradle 转发 | Gradle 透传 stdout | ✅ 不做编码转换 |
| 终端渲染 | Windows 终端代码页 | 可能是 GBK(936) 或 UTF-8(65001) |

**根因**：JVM 的 `file.encoding` 默认为系统编码（Windows 中文版为 GBK/CP936），而 Gradle 输出到终端时，终端可能使用不同编码。两者不一致导致乱码。

**修复方案**：
1. `gradle.properties` 中 `kotlin.daemon.jvmargs` 添加 `-Dfile.encoding=UTF-8`
2. `desktopApp/build.gradle.kts` 中 `application.jvmArgs` 添加 `-Dfile.encoding=UTF-8`
3. `org.gradle.jvmargs` 已含此参数，保持不变

---

## Issue #2: 托盘右键菜单中文显示为方块

**状态**：✅ 已修复
**发现版本**：第七步完成后
**现象**：系统托盘右键菜单的「显示窗口」和「退出」显示为方块（□□）。

**链路分析**：

```
Notifier.kt
  → MenuItem("显示窗口") 创建 AWT MenuItem
  → AWT 使用系统默认 GUI 字体渲染
  → Windows GDI 绘制菜单文本
  → 屏幕显示
```

| 环节 | 检查点 | 状态 |
|------|--------|------|
| 字符串内容 | Kotlin 中 `"显示窗口"` | ✅ 源码 UTF-8，运行时可正确读取 |
| AWT MenuItem 创建 | `MenuItem(String)` 构造 | ✅ 创建成功 |
| AWT 默认字体 | `MenuItem.getFont()` 默认值 | ❌ 可能为不包含 CJK 字形的字体（如 Dialog） |
| 系统字体可用性 | 系统是否安装了中文字体 | ✅ Windows 通常有 Microsoft YaHei/SimSun |

**根因**：AWT `MenuItem` 默认使用逻辑字体 `Dialog`，该字体在部分 Windows 系统上没有中文字形回退，导致 CJK 字符渲染为方块。

**修复方案**：在 `Notifier.kt` 中为 MenuItem 显式设置支持中文的字体（`Font("Microsoft YaHei", Font.PLAIN, 12)`）。
