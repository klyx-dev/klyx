package com.klyx.editor

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.compose.ui.unit.Density
import com.klyx.core.settings.EditorSettings
import com.klyx.editor.textaction.TextActionWindow
import io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.getComponent

class KlyxEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : CodeEditor(context, attrs, defStyleAttr, defStyleRes) {
    private val density = Density(context)

    private var textActions: TextActionWindow? = null

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        getComponent<EditorAutoCompletion>().apply {
            isEnabled = true
//            setLayout(AutoCompletionLayout())
//            setAdapter(AutoCompletionLayoutAdapter(density))
//            setEnabledAnimation(true)
        }

        getComponent<EditorDiagnosticTooltipWindow>().isEnabled = true
        getComponent<EditorTextActionWindow>().isEnabled = false

        lineNumberMarginLeft = 9f

        registerInlayHintRenderers(TextInlayHintRenderer.DefaultInstance, ColorInlayHintRenderer.DefaultInstance)
    }

    @OptIn(ExperimentalCodeEditorApi::class)
    @Suppress("RedundantSuspendModifier")
    suspend fun connectToLsp(state: CodeEditorState) {
        //
    }

    fun setTextActionWindow(window: TextActionWindow) {
        textActions = window
    }

    fun update(settings: EditorSettings) {
        tabWidth = settings.tabSize.toInt()
        props.apply {
            deleteEmptyLineFast = settings.deleteEmptyLineFast
            deleteMultiSpaces = settings.deleteMultiSpaces
            allowFullscreen = settings.allowFullscreen
            symbolPairAutoCompletion = settings.symbolPairAutoCompletion
            autoIndent = settings.autoIndent
            disallowSuggestions = settings.disallowSuggestions
            overScrollEnabled = settings.overScrollEnabled
            scrollFling = settings.scrollFling
            scrollAnimationDurationMs = settings.scrollAnimationDurationMs
            indicatorWaveLength = settings.indicatorWaveLength
            indicatorWaveWidth = settings.indicatorWaveWidth
            indicatorWaveAmplitude = settings.indicatorWaveAmplitude
            useICULibToSelectWords = settings.useICULibToSelectWords
            highlightMatchingDelimiters = settings.highlightMatchingDelimiters
            boldMatchingDelimiters = settings.boldMatchingDelimiters
            enableRoundTextBackground = settings.enableRoundTextBackground
            formatPastedText = settings.formatPastedText
            enhancedHomeAndEnd = settings.enhancedHomeAndEnd
            reselectOnLongPress = settings.reselectOnLongPress
            fastScrollSensitivity = settings.fastScrollSensitivity
            mouseWheelScrollFactor = settings.mouseWheelScrollFactor
            mouseMode = settings.mouseMode.value
            mouseModeAlwaysShowScrollbars = settings.mouseModeAlwaysShowScrollbars
            mouseContextMenu = settings.mouseContextMenu
            stickyScroll = settings.stickyScroll
            stickyScrollMaxLines = settings.stickyScrollMaxLines
            stickyScrollPreferInnerScope = settings.stickyScrollPreferInnerScope
            stickyScrollAutoCollapse = settings.stickyScrollAutoCollapse
            selectCompletionItemOnEnterForSoftKbd = settings.selectCompletionItemOnEnterForSoftKbd
        }
    }
}
