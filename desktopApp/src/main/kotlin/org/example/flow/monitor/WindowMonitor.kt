package org.example.flow.monitor

import com.sun.jna.ptr.IntByReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.example.flow.model.ActiveWindow

/**
 * 前台窗口监测器。
 * 通过 JNA 调用 Windows API，每 500ms 轮询一次当前前台窗口的标题和进程名。
 */
object WindowMonitor {

    /** 窗口标题缓冲区大小（字符数） */
    private const val TITLE_BUF_SIZE = 512

    /** 进程名缓冲区大小 */
    private const val PROCESS_BUF_SIZE = 260

    /** 轮询间隔（毫秒） */
    private const val POLL_INTERVAL_MS = 500L

    /**
     * 返回一个冷 Flow，每 [POLL_INTERVAL_MS] 毫秒发射一次当前前台窗口信息。
     * 收集者在协程中调用，取消协程即停止轮询。
     */
    fun observeActiveWindow(): Flow<ActiveWindow> = callbackFlow {
        while (true) {
            val hwnd = User32.INSTANCE.GetForegroundWindow()
            if (hwnd != null) {
                val title = getWindowTitle(hwnd)
                val processName = getProcessName(hwnd)
                trySend(ActiveWindow(title, processName, System.currentTimeMillis()))
            }
            delay(POLL_INTERVAL_MS)
        }
        awaitClose { /* 无资源需要释放 */ }
    }

    // ── 私有方法 ──────────────────────────────────────

    private fun getWindowTitle(hwnd: com.sun.jna.platform.win32.WinDef.HWND): String {
        val buffer = CharArray(TITLE_BUF_SIZE)
        val length = User32.INSTANCE.GetWindowTextW(hwnd, buffer, TITLE_BUF_SIZE)
        return if (length > 0) String(buffer, 0, length).trim() else ""
    }

    private fun getProcessName(hwnd: com.sun.jna.platform.win32.WinDef.HWND): String {
        return try {
            // 1. 获取进程 ID
            val pidRef = IntByReference()
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef)
            val pid = pidRef.value
            if (pid == 0) return "unknown"

            // 2. 打开进程句柄
            val handle = Kernel32.INSTANCE.OpenProcess(
                PROCESS_QUERY_INFORMATION or PROCESS_VM_READ,
                false,
                pid,
            ) ?: return "unknown"

            // 3. 获取进程可执行文件名
            val buffer = CharArray(PROCESS_BUF_SIZE)
            val length = Psapi.INSTANCE.GetModuleBaseNameW(handle, null, buffer, PROCESS_BUF_SIZE)

            // 4. 释放句柄
            Kernel32.INSTANCE.CloseHandle(handle)

            if (length > 0) String(buffer, 0, length).trim() else "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
