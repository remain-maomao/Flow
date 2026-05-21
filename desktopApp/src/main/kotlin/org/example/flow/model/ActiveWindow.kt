package org.example.flow.model

/**
 * 当前活跃窗口的信息快照。
 * @param title 窗口标题，如 "PLAN.md - Flow - IntelliJ IDEA"
 * @param processName 进程名，如 "idea64.exe"
 * @param timestamp 采集时间戳，System.currentTimeMillis()
 */
data class ActiveWindow(
    val title: String,
    val processName: String,
    val timestamp: Long,
)
