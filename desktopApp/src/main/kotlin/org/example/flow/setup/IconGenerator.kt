package org.example.flow.setup

import java.awt.*
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * 首次启动时自动生成水流主题应用图标。
 * 渐变蓝色圆角方形 + 白色波浪 + 水滴 + \"F\" 字母。
 */
object IconGenerator {
    private const val SIZE = 256
    private const val MARGIN = 8
    private const val RADIUS = 48f

    fun generateIfMissing() {
        val dir = File("desktopApp/src/main/resources")
        val file = File(dir, "icon.png")
        if (file.exists()) {
            println("[IconGenerator] Icon already exists: ${file.absolutePath}")
            return
        }
        dir.mkdirs()
        try {
            val image = generate()
            ImageIO.write(image, "PNG", file)
            println("[IconGenerator] Icon generated: ${file.absolutePath}")
        } catch (e: Exception) {
            println("[IconGenerator] Failed: ${e.message}")
        }
    }

    private fun generate(): BufferedImage {
        val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // 圆角裁剪
        val clip = RoundRectangle2D.Float(
            MARGIN.toFloat(), MARGIN.toFloat(),
            (SIZE - 2 * MARGIN).toFloat(), (SIZE - 2 * MARGIN).toFloat(),
            RADIUS, RADIUS,
        )
        g.clip = clip

        // 径向渐变背景
        val gradient = RadialGradientPaint(
            SIZE / 2f, SIZE / 3f, SIZE * 0.7f,
            floatArrayOf(0f, 0.55f, 1f),
            arrayOf(Color(0x42A5F5), Color(0x1E88E5), Color(0x0D47A1)),
        )
        g.paint = gradient
        g.fillRoundRect(0, 0, SIZE, SIZE, (RADIUS * 2).toInt(), (RADIUS * 2).toInt())

        // 波浪线条（白色半透明，三层叠加）
        g.clip = null
        val waveBaseY = SIZE * 0.62f
        for (i in 2 downTo 0) {
            g.color = Color(255, 255, 255, 60 + i * 25)
            g.stroke = BasicStroke(3.5f + i * 1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            drawSineWave(g, MARGIN.toFloat(), waveBaseY + i * 14, SIZE - MARGIN.toFloat(), waveBaseY + i * 14, 30f, 3f)
        }

        // 恢复裁剪
        g.clip = clip

        // 大水滴
        drawDrop(g, SIZE * 0.3f, SIZE * 0.20f, 16f, Color(255, 255, 255, 70))
        // 小水滴
        drawDrop(g, SIZE * 0.68f, SIZE * 0.16f, 10f, Color(255, 255, 255, 50))

        // 中心字母 \"F\"
        g.font = Font("Segoe UI", Font.BOLD, 110)
        val fm = g.fontMetrics
        val fx = (SIZE - fm.stringWidth("F")) / 2
        val fy = SIZE / 2 + fm.ascent / 2 - 8
        // 文字阴影
        g.color = Color(0, 0, 0, 40)
        g.drawString("F", fx + 3, fy + 3)
        // 文字本体
        g.color = Color(255, 255, 255, 230)
        g.drawString("F", fx, fy)

        // 底部小字 \"flow\"
        g.font = Font("Segoe UI", Font.ITALIC, 26)
        val fm2 = g.fontMetrics
        val fx2 = (SIZE - fm2.stringWidth("flow")) / 2
        g.color = Color(255, 255, 255, 120)
        g.drawString("flow", fx2, fy + 55)

        g.dispose()
        return image
    }

    private fun drawSineWave(
        g: Graphics2D, x1: Float, y1: Float, x2: Float, y2: Float,
        amplitude: Float, frequency: Float,
    ) {
        val path = Path2D.Float()
        path.moveTo(x1, y1)
        val steps = 60
        val dx = (x2 - x1) / steps
        for (i in 0..steps) {
            val x = x1 + i * dx
            val y = y1 + amplitude * Math.sin(i * frequency * Math.PI / steps).toFloat()
            path.lineTo(x, y)
        }
        g.draw(path)
    }

    private fun drawDrop(g: Graphics2D, cx: Float, cy: Float, radius: Float, color: Color) {
        g.color = color
        val r = radius.toInt()
        // 圆形主体
        g.fillOval((cx - r).toInt(), (cy - r).toInt(), r * 2, (r * 1.5f).toInt())
        // 尖端三角形
        val tipX = intArrayOf(
            (cx - r * 0.7f).toInt(),
            (cx + r * 0.7f).toInt(),
            cx.toInt(),
        )
        val tipY = intArrayOf(
            (cy - r).toInt(),
            (cy - r).toInt(),
            (cy - r * 1.8f).toInt(),
        )
        g.fillPolygon(tipX, tipY, 3)
    }
}
