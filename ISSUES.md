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

**状态**：✅ 已解决（英文降级）
**发现版本**：第七步完成后
**原因**：Compose Multiplatform 框架 Bug（[GitHub #4486](https://github.com/JetBrains/compose-multiplatform/issues/4486) → [YouTrack CMP-4486](https://youtrack.jetbrains.com/issue/CMP-4486)）。
Windows 系统托盘菜单由原生 Explorer.exe 渲染，AWT/Compose 层无法控制字体。所有 JDK 版本和编码设置均无效。

**解决方案**：托盘菜单改为英文 `"Show Window"` / `"Exit"`，ASCII 字符在所有字体中可正常渲染。

**未来**：等待 Compose Multiplatform 框架修复后改回中文。
