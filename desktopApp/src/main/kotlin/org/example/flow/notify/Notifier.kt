package org.example.flow.notify

import org.example.flow.model.Mode
import java.awt.*
import java.awt.image.BufferedImage

/**
 * 系统托盘通知封装。
 * 初始化托盘图标、气泡通知、右键菜单，图标颜色随模式变化。
 */
class Notifier(
    private val onShowWindow: () -> Unit,
    private val onExit: () -> Unit,
) {
    private var trayIcon: TrayIcon? = null

    init {
        if (SystemTray.isSupported()) {
            setupTray()
        } else {
            println("[Notifier] ⚠️ 系统托盘不支持")
        }
    }

    private fun setupTray() {
        try {
            val tray = SystemTray.getSystemTray()

            // 中文字体（解决菜单显示方块问题）
            val menuFont = Font("Microsoft YaHei", Font.PLAIN, 12)

            // 右键菜单
            val popup = PopupMenu()
            val showItem = MenuItem("显示窗口")
            showItem.font = menuFont
            showItem.addActionListener { onShowWindow() }
            val exitItem = MenuItem("退出")
            exitItem.font = menuFont
            exitItem.addActionListener { onExit() }
            popup.add(showItem)
            popup.addSeparator()
            popup.add(exitItem)

            // 创建初始绿色图标
            val image = createIcon(TRAY_GREEN)
            trayIcon = TrayIcon(image, "Flow - 专注助手", popup)
            trayIcon!!.isImageAutoSize = true
            tray.add(trayIcon!!)

            println("[Notifier] ✅ 托盘图标已创建")
        } catch (e: Exception) {
            println("[Notifier] ⚠️ 托盘初始化失败: ${e.message}")
        }
    }

    /** 弹出托盘气泡通知 */
    fun show(title: String, message: String) {
        trayIcon?.displayMessage(title, message, TrayIcon.MessageType.INFO)
        println("[Notifier] 📢 $title: $message")
    }

    /** 根据模式切换托盘图标颜色 */
    fun updateIcon(mode: Mode) {
        val color = if (mode == Mode.WORK) TRAY_GREEN else TRAY_RED
        trayIcon?.image = createIcon(color)
    }

    // ── 内部方法 ──────────────────────────────────────

    /** 程序生成 16×16 纯色圆形图标 */
    private fun createIcon(color: Color): Image {
        val size = 16
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = color
        g.fillOval(1, 1, size - 2, size - 2) // 留 1px 边距
        g.dispose()
        return image
    }

    companion object {
        private val TRAY_GREEN = Color(0x4CAF50)
        private val TRAY_RED = Color(0xF44336)
    }
}
