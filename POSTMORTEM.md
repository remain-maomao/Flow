# Postmortem: 分类 Bug 修复

## 问题

YouTube、微博等娱乐网站始终被识别为 WORK（工作模式），不切换为 ENTERTAINMENT。

## 时间线

| 轮次 | 尝试 | 结果 | 根因 |
|------|------|------|------|
| 1 | 加诊断日志 | 发现窗口轮询注入 WORK | — |
| 2 | `filter` 跳过浏览器窗口分类 | ❌ 无效 | merge 架构本身错误 |
| 3 | 暂停，重写需求 Spec | 产出 `CLASSIFIER_SPEC.md` | — |
| 4 | 按 Spec 重写：单入口分类 | ❌ YouTube 仍不行 | State 捕获陷阱 |
| 5 | `extensionConnected` 改为 `State<Boolean>` | ✅ 全部正常 | — |

## 两个 Bug 详解

### Bug 1: merge 设计缺陷

**现象**：
```
[Classifier] BROWSER url=youtube.com → ENTERTAINMENT  ← 扩展（正确）
[Classifier] WINDOW  title="YouTube - ..." → WORK     ← 窗口轮询（500ms 后）
[Classifier] WINDOW  title="YouTube - ..." → WORK     ← 持续打断
```

**根因**：`merge(windowResults, browserResults)` 将两个数据源合并，窗口轮询每 500ms 注入一次 WORK，ModeDetector 的 entertainment streak 被持续重置，5 秒永远达不到。

**为什么 filter 也不管用**：filter 只是跳过了一些分类结果，但 merge 的架构决定了两个源仍在并行产出数据；而且 filter 的 capture 语义也有问题。

**正确方案**：两个数据源**互斥使用**，不同时参与判定。

```
ActiveWindow 到达
  ├─ 进程是浏览器 + 扩展在线 → 用最新 BrowserMessage 分类
  └─ 否则 → 用窗口标题分类
```

### Bug 2: State 捕获陷阱

**现象**：UI 的「Browser Tab」卡片正确显示 `youtube.com`，但分类逻辑走的却是 WINDOW 路径。

**根因**：
```kotlin
// main.kt
var extensionConnected by remember { mutableStateOf(false) }
TabServer(onConnected = { extensionConnected = true })

// App.kt  
fun FlowApp(extensionConnected: Boolean) {   // ← 普通 Boolean 值
    LaunchedEffect(Unit) {
        // extensionConnected 在这里被捕获，永远是 false
        // TabServer 回调改了 main.kt 的值，但这里是副本
    }
}
```

**修复**：
```kotlin
// main.kt
val extensionConnectedState = remember { mutableStateOf(false) }
FlowApp(extensionConnected = extensionConnectedState)  // ← State 对象

// App.kt
fun FlowApp(extensionConnected: State<Boolean>) {
    LaunchedEffect(Unit) {
        extensionConnected.value  // ← 每次读取最新值
    }
}
```

## 关键教训

1. **数据源互斥 vs 合并**：当两个数据源覆盖同一个分类目标时，应互斥选择，不应合并。
2. **Compose 参数传递**：传给 LaunchedEffect 的普通值会被捕获。需要响应式读取时必须传 `State<T>`。
3. **先写 Spec，再写代码**：`CLASSIFIER_SPEC.md` 用原子步骤描述判定逻辑，大幅减少了返工。

## 相关文件

- `CLASSIFIER_SPEC.md` — 分类逻辑规格说明书
- `PHASE3_PLAN.md` — 第三阶段开发计划
- `ISSUES.md` — 问题追踪（Issue #1 控制台乱码、Issue #2 托盘菜单方块）
