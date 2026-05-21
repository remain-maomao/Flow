package org.example.flow.monitor

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary

/**
 * 本文件包含 WindowMonitor 所需的 Windows API 的 JNA 接口映射。
 * 仅声明 MVP 需要的 6 个函数，不引入过度抽象。
 */

// ── User32 ────────────────────────────────────────────
interface User32 : StdCallLibrary {
    companion object {
        val INSTANCE: User32 = Native.load("user32", User32::class.java)
    }

    /** 获取当前前台窗口句柄 */
    fun GetForegroundWindow(): HWND?

    /** 获取窗口标题（Unicode 版），返回实际拷贝的字符数 */
    fun GetWindowTextW(hWnd: HWND, lpString: CharArray, nMaxCount: Int): Int

    /** 获取窗口所属的线程 ID 和进程 ID */
    fun GetWindowThreadProcessId(hWnd: HWND, lpdwProcessId: IntByReference?): Int
}

// ── Kernel32 ──────────────────────────────────────────
interface Kernel32 : StdCallLibrary {
    companion object {
        val INSTANCE: Kernel32 = Native.load("kernel32", Kernel32::class.java)
    }

    fun OpenProcess(dwDesiredAccess: Int, bInheritHandle: Boolean, dwProcessId: Int): HANDLE?
    fun CloseHandle(hObject: HANDLE): Boolean
}

// ── Psapi ─────────────────────────────────────────────
interface Psapi : StdCallLibrary {
    companion object {
        val INSTANCE: Psapi = Native.load("Psapi", Psapi::class.java)
    }

    /**
     * 获取进程的可执行文件名。
     * @param hProcess 进程句柄
     * @param hModule 模块句柄，传 null 代表主模块
     * @param lpBaseName 输出缓冲区
     * @param nSize 缓冲区大小（字符数）
     * @return 成功返回字符串长度
     */
    fun GetModuleBaseNameW(
        hProcess: HANDLE,
        hModule: Pointer?,
        lpBaseName: CharArray,
        nSize: Int,
    ): Int
}

// ── 常量 ──────────────────────────────────────────────
/** OpenProcess 所需权限 */
const val PROCESS_QUERY_INFORMATION = 0x0400
const val PROCESS_VM_READ = 0x0010
