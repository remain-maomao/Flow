# Phase 4 总结：通知音效功能

## 日期

2026-05-22

## 需求

每次提醒弹窗时播放音效，配合表情包提升用户体验。

## 实施步骤（6 步）

| Step | 内容 | 文件 | 状态 |
|------|------|------|------|
| 1 | AppConfig 新增 `soundFolder` 字段 | `ConfigManager.kt` | ✅ |
| 2 | 创建 SoundPicker，随机选 WAV | `SoundPicker.kt`（新建） | ✅ |
| 3 | 创建 SoundPlayer，异步播放 | `SoundPlayer.kt`（新建） | ✅ |
| 4 | Settings 面板增加音效设置 UI | `SettingsPanel.kt` | ✅ |
| 5 | main.kt 集成：提醒时自动播放 | `main.kt` | ✅ |
| 6 | 端到端测试 | 手动触发 + 自然提醒 | ✅ |

## 遇到的问题 & 解决方案

### 问题 1：播放成功但听不到声音

**现象**：终端日志显示 SoundPlayer 完整执行了加载 → 打开 → 播放 → 完成，没有报任何异常，但用户听不到任何声音。

**排查过程**：
1. 在 main.kt 的 `onReminder` 回调中加日志，确认 `soundFolder` 读取正确
2. 在 SoundPicker 中已有日志，确认文件被正确随机选中
3. 在 SoundPlayer 中加详细日志：
   - 列出系统所有音频混音器（排查输出设备路由）
   - 打印音频格式详情（排查格式兼容性）
   - 检测 Clip 打开状态、运行状态
   - 检查并设置 MASTER_GAIN 音量控制

**根因**：`javax.sound.sampled.Clip` 默认音量是 0dB（中等音量）。在某些 Windows 音频配置下，这个默认值可能不够响，用户无法察觉。

**解决方案**：在 `SoundPlayer.play()` 中增加 `setVolumeToMax()` 方法，通过 `FloatControl.Type.MASTER_GAIN` 将音量推到最大值（+6.0206dB），音量翻倍后用户能清晰听到。
```
修复前: volume = 0.0dB
修复后: volume = 6.0206dB (max)
```

### 问题 2：配置路径丢失

**现象**：用户之前配置的表情包文件夹路径在重启后消失。

**根因**：与音效功能无关。是 Step 1 验证时执行了 `rm config.json` 来测试自动生成逻辑，删除了用户的配置文件。

**解决**：用户重新填写路径后，后续重启正常保留。

## 技术要点

- **技术选型**：`javax.sound.sampled`（JDK 内置），零外部依赖
- **音频格式**：WAV（原生支持），不支持时自动转 PCM_SIGNED
- **播放方式**：独立 `Thread` 异步播放，不阻塞 UI
- **容错设计**：播放失败仅打印日志，不影响弹窗显示
- **配置对称**：soundFolder 与 emojiFolder 设计完全对称，用户体验一致

## 改动文件清单

```
新建:
  desktopApp/src/main/kotlin/org/example/flow/notify/SoundPicker.kt
  desktopApp/src/main/kotlin/org/example/flow/notify/SoundPlayer.kt
  PHASE4_PLAN.md

修改:
  desktopApp/src/main/kotlin/org/example/flow/classify/ConfigManager.kt  (+1 field)
  desktopApp/src/main/kotlin/org/example/flow/ui/SettingsPanel.kt       (+Sound Folder UI)
  desktopApp/src/main/kotlin/org/example/flow/main.kt                   (+integration)
```

## 最终效果

- 用户将 WAV 文件放入 `~/.flow/sounds/`
- Settings 中配置路径
- 每次提醒弹窗出现时，随机播放一个音效
- 表情包 + 俏皮音效 = 让人会心一笑的提醒体验 🎵
