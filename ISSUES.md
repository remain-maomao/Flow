# Flow - 问题追踪

## Issue #1: Gradle 控制台日志中文乱码

**状态**：🟡 框架层面问题，应用层无法修复
**发现版本**：第七步完成后
**现象**：运行 `./gradlew :desktopApp:run` 后，终端输出的中文日志显示为乱码。

**尝试过的修复**：
| 方案 | 结果 |
|------|------|
| `-Dfile.encoding=UTF-8`（gradle.properties + build.gradle.kts） | ❌ 无效 |
| 删除所有 encoding 配置 | ❌ 无效 |
| `-Dfile.encoding=GBK`（build.gradle.kts） | ❌ 无效 |

**根因**：JVM/Gradle/Windows 终端三者间的编码桥接问题，非应用层代码能解决。不影响应用功能和用户 UI。

---

## Issue #2: 托盘右键菜单中文显示为方块

**状态**：✅ 已解决（英文降级）
**发现版本**：第七步完成后
**原因**：Compose Multiplatform 框架 Bug（[GitHub #4486](https://github.com/JetBrains/compose-multiplatform/issues/4486) → [YouTrack CMP-4486](https://youtrack.jetbrains.com/issue/CMP-4486)）。
Windows 系统托盘菜单由原生 Explorer.exe 渲染，AWT/Compose 层无法控制字体。所有 JDK 版本和编码设置均无效。

**解决方案**：托盘菜单改为英文 `"Show Window"` / `"Exit"`，ASCII 字符在所有字体中可正常渲染。

**未来**：等待 Compose Multiplatform 框架修复后改回中文。
