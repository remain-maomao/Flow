package org.example.flow.engine

/**
 * 提醒规则定义。
 *
 * @param id 唯一标识，如 "drink_water"
 * @param intervalMs 触发间隔（现实毫秒），如 15 分钟 = 15 * 60 * 1000
 * @param message 提醒文案
 * @param priority 优先级，数值越大越优先；同时触发多个提醒时仅展示最高优先级
 */
data class ReminderRule(
    val id: String,
    val intervalMs: Long,
    val message: String,
    val priority: Int = 0,
)
