package org.example.flow.notify

import org.example.flow.model.Mode
import java.awt.*
import java.awt.image.BufferedImage

/**
 * 系统托盘封装。
 * 管理托盘图标和右键菜单，图标颜色随模式变化。
 * 通知弹窗由 NotificationPopup 负责，不再使用原生气泡。
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
            println("[Notifier] System tray not supported")
        }
    }

    private fun setupTray() {
        try {
            val tray = SystemTray.getSystemTray()

            // 右键菜单（使用英文，系统托盘原生菜单不支持 AWT 中文字体设置）
            val popup = PopupMenu()
            val showItem = MenuItem("Show Window")
            showItem.addActionListener { onShowWindow() }
            val exitItem = MenuItem("Exit")
            exitItem.addActionListener { onExit() }
            popup.add(showItem)
            popup.addSeparator()
            popup.add(exitItem)

            // 加载应用图标
            val iconImage = try {
                val iconFile = java.io.File("desktopApp/src/main/resources/icon.png")
                if (iconFile.exists()) javax.imageio.ImageIO.read(iconFile)
                else createIcon(TRAY_GREEN)
            } catch (e: Exception) {
                createIcon(TRAY_GREEN)
            }

            trayIcon = TrayIcon(iconImage, "Flow", popup)
            trayIcon!!.isImageAutoSize = true
            tray.add(trayIcon!!)

            println("[Notifier] Tray icon created")
        } catch (e: Exception) {
            println("[Notifier] Tray init failed: ${e.message}")
        }
    }

    /** 更新托盘图标（当前始终使用应用图标） */
    fun updateIcon(mode: Mode) {
        // 托盘图标统一使用应用 logo，颜色不变。模式通过窗口 UI 指示。
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
