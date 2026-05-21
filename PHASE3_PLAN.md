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

### Phase 3.2：UI 重构（P1）

#### 3.2.1 Tab 式导航

**问题**：设置面板嵌入首页，占用大量空间，且与首页信息混杂。

**方案**：
- 顶部新增 Tab 栏：「Dashboard」/「Settings」
- Dashboard 显示：模式条 + 计时 + 窗口监测 + 浏览器 + 扩展引导
- Settings 显示：黑名单编辑面板
- 开发者模式下的倍速滑块和手动触发按钮放在 Dashboard 底部

**产出**：
- 重构 `ui/App.kt`，提取 Dashboard 和 Settings 为独立 Composable
- 新增 `ui/DashboardPanel.kt`

**验证**：
- 点击 Tab 切换，Dashboard 和 Settings 各自独立显示
- 设置面板不再挤占 Dashboard 空间
- 两个 Tab 内容各自可滚动

---

### Phase 3.3：分类精度提升（P1）

#### 3.3.1 白名单功能（URL 路径匹配）

**问题**：YouTube/B站 有些频道是学习内容（如教程、课程），应识别为工作模式。目前只看域名，无法区分。

**方案**：
- `AppConfig` 新增 `whitelistUrls: List<String>`
- 白名单匹配规则：URL 包含指定字符串 → 强制 WORK
- 优先级：白名单 > 黑名单
- 预置示例：
  - `youtube.com/@freecodecamp`
  - `youtube.com/@Fireship`
  - `bilibili.com/v/education`
- 设置面板新增白名单编辑区

**数据流**：
```
浏览器 URL → 白名单匹配 → 命中 → WORK
           → 未命中 → 黑名单匹配 → 命中 → ENTERTAINMENT
                                   → 未命中 → WORK
```

**修改文件**：
- `classify/ConfigManager.kt` — AppConfig 加字段
- `classify/ModeClassifier.kt` — 白名单优先匹配
- `ui/SettingsPanel.kt` — 新增白名单编辑

**验证**：
| 操作 | 预期 |
|------|------|
| 添加白名单 `youtube.com/@freecodecamp` | 访问该频道 → 工作模式 |
| 访问其他 YouTube 视频 | 仍是娱乐模式 |
| 删除白名单项 | 该频道恢复娱乐模式 |

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
