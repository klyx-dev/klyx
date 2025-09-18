package com.klyx.editor.signature

import android.graphics.Rect
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.klyx.editor.KlyxEditor
import com.klyx.editor.compose.ComposeInfoPopup
import org.eclipse.lsp4j.SignatureHelp

class SignatureHelpWindow(
    editor: KlyxEditor,
    localView: View,
    compositionContext: CompositionContext
) : ComposeInfoPopup(editor, localView, compositionContext) {

    private var signatureHelp by mutableStateOf<SignatureHelp?>(null)

    private val maxWidth = (editor.width * 0.67).toInt()

    @Composable
    override fun Content() {
        val density = LocalDensity.current

        SignatureHelpContent(
            signatureHelp = signatureHelp,
            maxWidth = with(density) { maxWidth.toDp() },
            maxHeight = 235.dp
        )
    }

    fun show(signatureHelp: SignatureHelp) {
        if (signatureHelp.signatures == null || signatureHelp.activeSignature == null || signatureHelp.activeParameter == null) {
            dismiss()
            return
        }

        this.signatureHelp = signatureHelp
        updateWindowSizeAndLocation()
        show()
    }

    private fun updateWindowSizeAndLocation() {
        popup.contentView.measure(
            View.MeasureSpec.makeMeasureSpec(10000000, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(100000000, View.MeasureSpec.AT_MOST)
        )

        val width = popup.contentView.measuredWidth
        val height = popup.contentView.measuredHeight

        if (width == 0 || height == 0) {
            setSize(maxWidth, editor.height)
        } else {
            setSize(width, height)
        }
        updateWindowPosition()
    }

    private fun updateWindowPosition() {
        val locationBuffer = IntArray(2)
        val selection = editor.cursor.left()
        val charX = editor.getCharOffsetX(selection.line, selection.column)
        val charY = editor.getCharOffsetY(
            selection.line,
            selection.column
        ) - editor.rowHeight - 10 * editor.dpUnit
        editor.getLocationInWindow(locationBuffer)
        val restAbove = charY + locationBuffer[1]
        val restBottom = editor.height - charY - editor.rowHeight
        val windowY = if (restAbove > restBottom) {
            charY - height
        } else {
            charY + editor.rowHeight * 1.5f
        }
        if (windowY < 0) {
            dismiss()
            return
        }
        val windowX = (charX - width / 2).coerceAtLeast(0f)
        setLocationAbsolutely(windowX.toInt(), windowY.toInt())
    }
}
