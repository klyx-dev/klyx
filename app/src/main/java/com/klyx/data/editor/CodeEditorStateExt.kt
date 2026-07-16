package com.klyx.data.editor

import com.klyx.api.data.preferences.EditorSettings
import io.github.rosemoe.sora.compose.CodeEditorState

fun CodeEditorState.applyEditorSettings(settings: EditorSettings) {
    tabWidth = settings.tabSize
    isLineNumberPinned = settings.pinLineNumbers
    isWordwrap = settings.wordWrap

    props.apply {
        deleteEmptyLineFast = settings.deleteEmptyLineFast
        deleteMultiSpaces = settings.deleteMultiSpaces
        symbolPairAutoCompletion = settings.symbolPairAutoCompletion
        autoIndent = settings.autoIndent
        disallowSuggestions = settings.disallowSuggestions
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
