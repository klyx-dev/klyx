package com.klyx.editor.lsp.editor

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View.MeasureSpec
import android.widget.TextView
import com.klyx.editor.R
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.subscribeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureInformation

open class SignatureHelpWindow(
    private val editor: CodeEditor
) : EditorPopupWindow(editor, FEATURE_HIDE_WHEN_FAST_SCROLL or FEATURE_SCROLL_AS_CONTENT) {
    private var signatureBackgroundColor = 0
    private var highlightParameter = 0
    private var defaultTextColor = 0

    @SuppressLint("InflateParams")
    private val rootView = LayoutInflater.from(editor.context)
        .inflate(R.layout.signature_help_tooltip_window, null, false)

    private val maxWidth = (editor.width * 0.67).toInt()
    private val maxHeight = (editor.dpUnit * 235).toInt()

    private val text: TextView = rootView.findViewById(R.id.signature_help_tooltip_text)
    private val locationBuffer = IntArray(2)
    protected val eventManager = editor.createSubEventManager()

    private lateinit var signatureHelp: SignatureHelp


    init {
        super.setContentView(rootView)

        eventManager.subscribeEvent<ColorSchemeUpdateEvent> { _, _ ->
            applyColorScheme()
        }

        applyColorScheme()
    }

    fun isEnabled() = eventManager.isEnabled

    fun setEnabled(enabled: Boolean) {
        eventManager.isEnabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

    open fun show(signatureHelp: SignatureHelp) {
        this.signatureHelp = signatureHelp

        if (signatureHelp.signatures == null || signatureHelp.activeSignature == null || signatureHelp.activeParameter == null) {
            dismiss()
            return
        }

        renderSignatureHelp()
        updateWindowSizeAndLocation()
        show()
    }


    private fun updateWindowSizeAndLocation() {
        rootView.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        )

        val width = rootView.measuredWidth
        val height = rootView.measuredHeight

        setSize(width, height)

        updateWindowPosition()
    }

    protected open fun updateWindowPosition() {
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

    private fun renderSignatureHelp() {
        val activeSignatureIndex = signatureHelp.activeSignature
        val activeParameterIndex = signatureHelp.activeParameter
        val signatures = signatureHelp.signatures

        val renderStringBuilder = SpannableStringBuilder()

        if (activeSignatureIndex < 0 || activeParameterIndex < 0) {
            Log.d("SignatureHelpWindow", "activeSignature or activeParameter is negative")
            return
        }

        if (activeSignatureIndex >= signatures.size) {
            Log.d("SignatureHelpWindow", "activeSignature is out of range")
            return
        }

        // Get only the activated signature
        for (i in 0..activeSignatureIndex) {
            formatSignature(
                signatures[i],
                activeParameterIndex,
                renderStringBuilder,
                isCurrentSignature = i == activeSignatureIndex
            )
            if (i < activeSignatureIndex) {
                renderStringBuilder.append("\n")
            }
        }

        text.text = renderStringBuilder
    }

    private fun formatSignature(
        signature: SignatureInformation,
        activeParameterIndex: Int,
        renderStringBuilder: SpannableStringBuilder,
        isCurrentSignature: Boolean
    ) {
        val label = signature.label
        val parameters = signature.parameters
        val activeParameter = parameters.getOrNull(activeParameterIndex)

        val parameterStart = label.substring(0, label.indexOf('('))
        val currentIndex = 0.coerceAtLeast(renderStringBuilder.lastIndex);

        renderStringBuilder.append(
            parameterStart,
            ForegroundColorSpan(defaultTextColor), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        renderStringBuilder.append(
            "("
        )

        for (i in 0 until parameters.size) {
            val parameter = parameters[i]
            if (parameter == activeParameter && isCurrentSignature) {
                renderStringBuilder.append(
                    parameter.label.left,
                    ForegroundColorSpan(highlightParameter),
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                renderStringBuilder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    renderStringBuilder.lastIndex - parameter.label.left.length,
                    renderStringBuilder.lastIndex,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (i != parameters.size - 1) {
                    renderStringBuilder.append(
                        ", ", ForegroundColorSpan(highlightParameter),
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else {
                renderStringBuilder.append(
                    parameter.label.left,
                    ForegroundColorSpan(defaultTextColor),
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (i != parameters.size - 1 && (!isCurrentSignature || parameter != activeParameter)) {
                renderStringBuilder.append(", ")
            }
        }

        renderStringBuilder.append(")")

        if (isCurrentSignature) {
            renderStringBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                currentIndex,
                renderStringBuilder.lastIndex,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    }

    private fun applyColorScheme() {
        val colorScheme = editor.colorScheme
        text.typeface = editor.typefaceText
        defaultTextColor = colorScheme.getColor(EditorColorScheme.SIGNATURE_TEXT_NORMAL)
        highlightParameter = colorScheme.getColor(EditorColorScheme.SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER)
        signatureBackgroundColor = colorScheme.getColor(EditorColorScheme.SIGNATURE_BACKGROUND)

        val background = GradientDrawable()
        background.cornerRadius = editor.dpUnit * 8
        background.setColor(colorScheme.getColor(EditorColorScheme.SIGNATURE_BACKGROUND))
        rootView.background = background

        if (isShowing) {
            renderSignatureHelp()
        }
    }
}
