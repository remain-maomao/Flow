package org.example.flow.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.flow.model.Mode

/**
 * 提醒引擎：模式驱动的定时提醒 + 演示时间加速。
 *
 * 计时说明：
 * - 所有 intervalMs 均为"现实世界时间"，如 15 分钟 = 15 * 60 * 1000
 * - timeScale 为加速倍率：60 → 1 现实秒 = 1 虚拟分钟；1 → 真实时间
 * - 实际 delay = intervalMs / timeScale
 */
class ReminderEngine(
    private var timeScale: Long = 60L,
    private val onReminder: (ReminderRule) -> Unit = {},
) {
    // ── 提醒规则 ──────────────────────────────────────
    private val workRules = listOf(
        ReminderRule("drink", 15 * 60_000, "💧 该喝水了", priority = 0),
        ReminderRule("stand", 40 * 60_000, "🧍 站起来活动一下", priority = 1),
    )
    private val entRules = listOf(
        ReminderRule("ent_nudge", 2 * 60_000, "⏰ 已经过去 2 分钟了", priority = 0),
    )

    // ── 内部状态 ──────────────────────────────────────
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val jobs = mutableListOf<Job>()

    private var currentMode = Mode.WORK
    private var modeStartRealMs = System.currentTimeMillis()

    private val _elapsedVirtualMs = MutableStateFlow(0L)
    /** 当前模式下已持续的虚拟毫秒数 */
    val elapsedVirtualMs: StateFlow<Long> = _elapsedVirtualMs.asStateFlow()

    private val _nextReminderVirtualMs = MutableStateFlow<Long?>(null)
    /** 距离下次提醒的虚拟毫秒数，null 表示无提醒规则 */
    val nextReminderVirtualMs: StateFlow<Long?> = _nextReminderVirtualMs.asStateFlow()

    // ── 公开方法 ──────────────────────────────────────

    /** 首次启动引擎（无条件启动计时器，不检查模式是否变化） */
    fun start() {
        println("[ReminderEngine] Engine started, mode=$currentMode, timeScale=${timeScale}x")
        restartTimers()
    }

    /** 模式变化时调用，重置所有计时器 */
    fun onModeChanged(newMode: Mode) {
        if (newMode == currentMode) return
        currentMode = newMode
        println("[ReminderEngine] Mode changed: $currentMode")
        restartTimers()
    }

    /** 更新时间倍速，重置计时器 */
    fun updateTimeScale(newScale: Long) {
        if (newScale <= 0) return
        timeScale = newScale
        println("[ReminderEngine] TimeScale changed: ${newScale}x")
        restartTimers()
    }

    /** 获取当前模式 */
    fun getCurrentMode(): Mode = currentMode

    /** 手动触发一次当前模式的第一个提醒（用于测试按钮） */
    fun triggerNow(): ReminderRule {
        val rules = currentRules()
        val rule = rules.first()
        println("[ReminderEngine] Manual trigger: ${rule.message}")
        onReminder(rule)
        return rule
    }

    /** 释放资源 */
    fun dispose() {
        cancelAllJobs()
        scope.cancel()
    }

    // ── 私有方法 ──────────────────────────────────────

    private fun currentRules(): List<ReminderRule> = when (currentMode) {
        Mode.WORK -> workRules
        Mode.ENTERTAINMENT -> entRules
    }

    private fun restartTimers() {
        cancelAllJobs()
        modeStartRealMs = System.currentTimeMillis()
        startTimers()
    }

    private fun cancelAllJobs() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    private fun startTimers() {
        val rules = currentRules()

        // 1. 为每条规则启动独立协程：delay → 回调 → 循环
        rules.forEach { rule ->
            val job = scope.launch {
                while (isActive) {
                    val delayMs = rule.intervalMs / timeScale
                    if (delayMs > 0) {
                        delay(delayMs)
                    } else {
                        delay(1) // 倍速太高时至少等 1ms
                    }
                    if (isActive) {
                        println("[ReminderEngine] Reminder fired: ${rule.message}")
                        onReminder(rule)
                    }
                }
            }
            jobs.add(job)
        }

        // 2. 启动 ticker 协程：更新 UI 状态（已持续时长、下次提醒倒计时）
        val tickerJob = scope.launch {
            while (isActive) {
                val realElapsed = System.currentTimeMillis() - modeStartRealMs
                val virtualElapsed = realElapsed * timeScale
                _elapsedVirtualMs.value = virtualElapsed

                // 计算距离最近的下一次提醒还剩多少虚拟毫秒
                if (rules.isNotEmpty()) {
                    val nextIn = rules.minOfOrNull { rule ->
                        val cyclesCompleted = virtualElapsed / rule.intervalMs
                        val nextTrigger = (cyclesCompleted + 1) * rule.intervalMs
                        (nextTrigger - virtualElapsed).coerceAtLeast(0)
                    }
                    _nextReminderVirtualMs.value = nextIn
                } else {
                    _nextReminderVirtualMs.value = null
                }

                delay(200) // UI 刷新频率：每 200ms
            }
        }
        jobs.add(tickerJob)
    }

    // ── 格式化工具 ────────────────────────────────────

    companion object {
        /** 毫秒 → "X分 Y秒" */
        fun formatDuration(ms: Long): String {
            val totalSec = ms / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return if (min > 0) "${min}分 ${sec}秒" else "${sec}秒"
        }
    }
}
