package com.klyx.core.platform

import com.klyx.core.composeWindowProvider
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.Timer

actual fun Platform.showToast(
    message: String,
    duration: ToastDuration
) {
    val parent = composeWindowProvider.invoke()
    val duration = when (duration) {
        ToastDuration.Short -> 2000
        ToastDuration.Long -> 5000
    }

    if (parent != null) {
        val toast = JWindow(parent).apply {
            background = Color(0, 0, 0, 0) // Transparent window
        }

        val panel = RoundedPanel(arcWidth = 28, arcHeight = 28)
        val label = JLabel(message).apply {
            foreground = Color.WHITE
            background = Color.BLACK
            isOpaque = false
            border = BorderFactory.createEmptyBorder(10, 24, 10, 24)
        }
        panel.add(label)
        toast.contentPane.add(label)
        toast.pack()

        // Position at bottom center inside the parent window
        val parentBounds = parent.bounds
        val x = parentBounds.x + (parentBounds.width - toast.width) / 2
        val y = parentBounds.y + parentBounds.height - toast.height - 40
        toast.setLocation(x, y)
        toast.isVisible = true

        Timer(duration) { toast.dispose() }.start()
    } else {
        // Fallback: show dialog if window reference is missing
        JOptionPane.showMessageDialog(null, message)
    }
}

// Utility class for rounded toast background
private class RoundedPanel(
    private val arcWidth: Int = 24,
    private val arcHeight: Int = 24,
    private val bgColor: Color = Color(50, 50, 50, 220)
) : JPanel() {
    init {
        isOpaque = false // Allow transparency
        layout = FlowLayout()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = bgColor
        g2.fill(
            RoundRectangle2D.Float(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                arcWidth.toFloat(),
                arcHeight.toFloat()
            )
        )
        super.paintComponent(g)
    }
}
