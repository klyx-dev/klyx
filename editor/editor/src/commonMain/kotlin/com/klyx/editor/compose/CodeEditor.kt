package com.klyx.editor.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.klyx.editor.compose.draw.EditorLayout

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    colorScheme: EditorColorScheme = EditorDefaults.colorScheme(),
    state: CodeEditorState = rememberCodeEditorState(editable = editable),
    showLineNumber: Boolean = true,
    pinLineNumber: Boolean = true,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    ProvideEditorCompositionLocals(
        colorScheme = colorScheme
    ) {
        CodeEditorImpl(
            modifier = modifier,
            state = state.apply { this.colorScheme = colorScheme },
            editable = editable,
            showLineNumber = showLineNumber,
            pinLineNumber = pinLineNumber,
            fontFamily = fontFamily,
            fontSize = fontSize
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CodeEditorImpl(
    modifier: Modifier,
    state: CodeEditorState,
    editable: Boolean,
    showLineNumber: Boolean,
    pinLineNumber: Boolean,
    fontFamily: FontFamily,
    fontSize: TextUnit
) {
    if (state.isBufferLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column {
                CircularWavyProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading")
            }
        }
        return
    }

    val scope = currentRecomposeScope

    LaunchedEffect(fontFamily) {
        state.invalidateDraw()
        scope.invalidate()
    }

    EditorLayout(
        state = state,
        modifier = modifier
            .clipToBounds()
            .pointerHoverIcon(PointerIcon.Text, overrideDescendants = true),
        editable = editable,
        showLineNumber = showLineNumber,
        pinLineNumber = pinLineNumber,
        fontFamily = fontFamily,
        fontSize = fontSize
    )
}
