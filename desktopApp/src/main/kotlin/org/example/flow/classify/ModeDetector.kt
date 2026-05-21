package org.example.flow.classify

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.example.flow.model.ClassificationResult
import org.example.flow.model.Mode

/**
 * 模式检测器：对分类结果做 5 秒防抖，避免窗口快速切换导致模式抖动。
 *
 * 防抖规则：
 * - 当前模式是 WORK，只有当连续 [debounceMs] 毫秒的分类结果都是 ENTERTAINMENT 时才切换到 ENTERTAINMENT
 * - 当前模式是 ENTERTAINMENT，只有当连续 [debounceMs] 毫秒的分类结果都是 WORK 时才切回 WORK
 * - 中间出现任何一次与当前模式相同的分类，计时器重置
 */
class ModeDetector(private val debounceMs: Long = 5_000L) {

    /**
     * @param input 来自 ModeClassifier 的原始分类流
     * @return 经过防抖的稳定模式流，初始值为 Mode.WORK
     */
    fun detect(input: Flow<ClassificationResult>): Flow<Mode> = flow {
        var currentMode = Mode.WORK
        var streakStartMs = 0L           // 当前异模式连续开始的时刻
        var streakTargetMode: Mode? = null // 正在累积的模式（与 currentMode 不同）

        emit(currentMode) // 立即发射初始状态

        input.collect { result ->
            val now = System.currentTimeMillis()

            if (result.mode == currentMode) {
                // 与当前模式一致 → 重置异模式计时
                streakTargetMode = null
                return@collect
            }

            // 与当前模式不同 → 开始或延续异模式计时
            if (streakTargetMode == result.mode) {
                // 延续同一异模式
                val elapsed = now - streakStartMs
                if (elapsed >= debounceMs) {
                    // 防抖时间到，切换模式
                    currentMode = result.mode
                    streakTargetMode = null
                    emit(currentMode)
                }
            } else {
                // 新异模式出现（或首次检测到异模式）
                streakTargetMode = result.mode
                streakStartMs = now
            }
        }
    }
}
