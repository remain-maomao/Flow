# Flow - 第四阶段开发计划

## 需求

每次提醒弹窗（表情包/文字弹窗）时，播放音效，增强用户体验。

## 设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 音频格式 | WAV | `javax.sound.sampled` 原生支持，零额外依赖 |
| 音效来源 | 用户自备 + 默认静音 | 不捆绑音频文件，降低包体积；用户按喜好定制 |
| 播放方式 | 异步 Clip 播放 | 不阻塞 UI，不阻塞提醒逻辑 |
| 配置路径 | `~/.flow/sounds/`（默认） | 与 emojis 文件夹对称，统一的资源管理模式 |
| 配置项 | `soundFolder: String = ""` | 空字符串 = 静音，向后兼容 |
| 音量控制 | MVP 不做 | 用户可通过系统音量控制 |

## 分步实施

---

### Step 1：AppConfig 增加 soundFolder 字段

**目标**：配置数据模型支持音效文件夹路径。

**修改文件**：`classify/ConfigManager.kt`

**操作**：
- `AppConfig` data class 新增字段：`val soundFolder: String = ""`
- 空字符串表示未设置，默认静音

**验证**：
| # | 操作 | 预期 |
|---|------|------|
| 1 | `./gradlew :desktopApp:compileKotlin` | 编译通过 |
| 2 | 删除 `~/.flow/config.json`，启动应用 | 自动生成含 `"soundFolder": ""` 的 config.json |
| 3 | 应用正常启动，无报错 | 日志无异常 |

---

### Step 2：创建 SoundPicker 工具类

**目标**：从音效文件夹中随机选取一个 WAV 文件。

**新建文件**：`notify/SoundPicker.kt`

**操作**：
```kotlin
object SoundPicker {
    fun pick(folderPath: String): File? {
        // 1. 如果路径为空，返回 null
        // 2. 如果文件夹不存在，返回 null
        // 3. 列出 .wav 文件
        // 4. 随机选一个，返回 File 对象
        // 5. 无文件 → 返回 null
    }
}
```

**验证**：
| # | 操作 | 预期 |
|---|------|------|
| 1 | `./gradlew :desktopApp:compileKotlin` | 编译通过 |
| 2 | 启动应用，未创建 sounds 文件夹 | 日志 `[SoundPicker] No folder configured` |
| 3 | 创建 `~/.flow/sounds/` 放入 test.wav | 调用 pick 能随机返回文件 |
| 4 | sounds 文件夹为空 | 日志 `[SoundPicker] No WAV files found`，返回 null |

---

### Step 3：创建 SoundPlayer 播放器

**目标**：用 javax.sound.sampled 异步播放 WAV，不阻塞 UI。

**新建文件**：`notify/SoundPlayer.kt`

**操作**：
```kotlin
object SoundPlayer {
    fun play(file: File) {
        // 1. 在独立线程中打开 AudioInputStream
        // 2. 获取 Clip，打开音频流
        // 3. 播放（clip.start()）
        // 4. 播放完毕后自动关闭 clip 和 stream
        // 5. 异常时打印日志，不崩溃
    }
}
```

**关键点**：
- 线程：`Thread { ... }.start()` 或 `Dispatchers.IO`
- 异常兜底：try-catch 包裹整段，失败不影响主流程
- 资源释放：播放完成后 `clip.close()` + `stream.close()`

**验证**：
| # | 操作 | 预期 |
|---|------|------|
| 1 | `./gradlew :desktopApp:compileKotlin` | 编译通过 |
| 2 | 单独写一段测试代码，播放一个已知 WAV 文件 | 听到声音，无异常 |
| 3 | 播放一个不存在的文件 | 日志报错但不崩溃 |
| 4 | 连续快速调用 play 多次 | 多个声音依次播放，不卡顿 |

---

### Step 4：SettingsPanel 增加音效设置 UI

**目标**：用户在设置页面可以配置音效文件夹路径。

**修改文件**：`ui/SettingsPanel.kt`

**操作**：
- 在「Emoji Folder」下方新增一行「Sound Folder」
- 布局与 Emoji Folder 一致：标签 + 路径输入框 + 两个按钮
  - 「Open」按钮 → 打开文件夹选择器
  - 「Clear」按钮 → 清空路径
  - 「Test」按钮 → 随机播放一个音效（预览用）
- 自动保存逻辑与 emojiFolder 一致

**验证**：
| # | 操作 | 预期 |
|---|------|------|
| 1 | `./gradlew :desktopApp:compileKotlin` | 编译通过 |
| 2 | 启动应用 → Settings Tab | 看到 Sound Folder 设置行，在 Emoji Folder 下方 |
| 3 | 点击 Open → 选择一个文件夹 | 输入框显示路径，config.json 自动更新 |
| 4 | 点击 Clear | 输入框清空，config.json 中 soundFolder 变为 "" |
| 5 | 切换到 Dashboard 再切回 Settings | 路径保持 |
| 6 | 关闭应用重新打开 | soundFolder 配置持久保留 |
| 7 | 点击 Test（文件夹中有 wav） | 播放随机音效 |
| 8 | 点击 Test（文件夹为空或无 wav） | 无声音，无崩溃 |

---

### Step 5：main.kt 集成音效播放

**目标**：提醒触发时，自动播放音效。

**修改文件**：`main.kt`

**操作**：
- `onReminder` 回调中，在设置 `notification = ...` 之前或之后：
  1. 从 ConfigManager 读取 `soundFolder`
  2. 调用 `SoundPicker.pick(soundFolder)` 获取文件
  3. 如果文件不为 null，调用 `SoundPlayer.play(file)`
- 播放与设置通知状态相互独立，互不影响

**关键点**：
- 不需要修改 NotificationState 结构（音效和弹窗是独立的）
- 播放失败不影响弹窗显示

**验证**：
| # | 操作 | 预期 |
|---|------|------|
| 1 | `./gradlew :desktopApp:compileKotlin` | 编译通过 |
| 2 | 未配置音效文件夹，手动触发提醒 | 弹窗正常显示，无声音，无报错（向后兼容） |
| 3 | 配置音效文件夹（有 wav），手动触发提醒 | 弹窗显示 + 播放随机音效 |
| 4 | 多次手动触发提醒 | 每次随机播放不同音效 + 弹窗 |
| 5 | 等自然提醒触发（不改倍速等 15 分钟） | 弹窗 + 音效正常 |
| 6 | 删除 sounds 文件夹中所有 wav | 弹窗正常，无声音，无报错 |

---

### Step 6：端到端场景测试

**目标**：完整场景验证，确保音效与表情包协同工作良好。

**场景 A：工作模式提醒**
| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 当前模式为 WORK | 状态栏绿色 |
| 2 | 等待 15 分钟（或改倍速加速） | 💧 该喝水了 弹窗 + 轻柔音效 |
| 3 | 等待 40 分钟 | 🧍 站起来 弹窗 + 轻柔音效 |

**场景 B：娱乐模式提醒**
| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 切换到 YouTube | 模式变为 ENTERTAINMENT（红色） |
| 2 | 等待 2 分钟 | ⏰ 弹窗 + 俏皮音效 |

**场景 C：表情包 + 音效结合**
| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | emojiFolder 有图片，soundFolder 有音效 | 弹窗显示随机表情 + 同时播放随机音效 |
| 2 | emojiFolder 有图片，soundFolder 为空 | 弹窗显示随机表情，无音效 |
| 3 | emojiFolder 为空，soundFolder 有音效 | 纯文字弹窗 + 音效 |
| 4 | 两者都为空 | 纯文字弹窗，无音效（完全向后兼容） |

**场景 D：性能与稳定性**
| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 连续快速手动触发 5 次提醒 | 5 个弹窗依次出现，5 个音效依次播放，无卡顿 |
| 2 | 音效播放中关闭应用 | 正常退出，无残留进程 |
| 3 | sounds 文件夹有损坏的 wav 文件 | 该文件跳过，日志报错但不崩溃 |

---

## 实施顺序

```
Step 1 (Config) → Step 2 (Picker) → Step 3 (Player) → Step 4 (Settings UI) → Step 5 (Integration) → Step 6 (E2E Test)
```

每个 Step 完成后执行对应验证，全部通过再进入下一 Step。

---

## 推荐音效素材

以下风格与表情包搭配效果极佳，供你找素材时参考：

| 风格 | 适合场景 | 推荐关键词 |
|------|---------|-----------|
| 水滴 / 气泡 | 💧 喝水提醒 | water drop, bubble pop |
| 柔和风铃 | 🧍 站起来 | soft bell, wind chime, gentle ding |
| 卡通弹跳 | ⏰ 娱乐 2 分钟 | cartoon boing, spring bounce |
| 像素游戏 | 通用 | 8-bit coin, retro pickup |
| 猫咪叫声 | 通用（萌系） | cat meow, kitten squeak |

**格式要求**：WAV，时长 ≤ 2 秒，不要太响太刺耳。
