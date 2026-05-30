package com.klyx.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.data.editor.applyEditorSettings
import com.klyx.data.editor.rememberEditorColorScheme
import com.klyx.data.preferences.EditorSettings
import com.klyx.data.preferences.LocalAppSettings
import com.klyx.ui.provider.LocalTreeSitter
import io.github.rosemoe.sora.compose.CodeEditor
import io.github.rosemoe.sora.compose.invalidate
import io.github.rosemoe.sora.compose.rememberCodeEditorState
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer

@Composable
fun CodeEditorDemo(
    modifier: Modifier = Modifier,
    settings: EditorSettings = LocalAppSettings.current.editor,
    fontSize: TextUnit = settings.fontSize.sp,
    fontFamily: FontFamily = settings.currentFontFamily,
    indicatorWaveLength: Float = settings.indicatorWaveLength,
    indicatorWaveWidth: Float = settings.indicatorWaveWidth,
    indicatorWaveAmplitude: Float = settings.indicatorWaveAmplitude,
    readOnly: Boolean = true
) {
    val indent = " ".repeat(settings.tabSize)

    val sampleCode = """
        #include <stdio.h>

        int main() {
        ${indent}printf("Hello, World!\n");
        ${indent}return 0;
        }

    """.trimIndent()

    val state = rememberCodeEditorState(sampleCode)

    fun applyDiagnostics() {
        val diagnostics = DiagnosticsContainer()
        diagnostics.addDiagnostic(
            DiagnosticRegion(
                41 + indent.length,
                56 + indent.length,
                DiagnosticRegion.SEVERITY_TYPO
            )
        )
        state.diagnostics = diagnostics
    }

    LaunchedEffect(sampleCode) {
        if (state.text.toString() != sampleCode) {
            state.setText(sampleCode)
            applyDiagnostics()
        }
    }

    LaunchedEffect(settings) {
        state.applyEditorSettings(settings)
    }

    LaunchedEffect(indicatorWaveLength, indicatorWaveWidth, indicatorWaveAmplitude) {
        state.props.apply {
            this.indicatorWaveLength = indicatorWaveLength
            this.indicatorWaveWidth = indicatorWaveWidth
            this.indicatorWaveAmplitude = indicatorWaveAmplitude
        }
        state.invalidate()
    }

    val density = LocalDensity.current
    val ts = LocalTreeSitter.current
    LaunchedEffect(Unit) {
        state.lineNumberMarginLeft = with(density) { 5.dp.toPx() }
        state.editorLanguage = ts.c()
        applyDiagnostics()
    }

    val colorScheme = rememberEditorColorScheme()

    LaunchedEffect(colorScheme) {
        state.colorScheme = colorScheme
    }

    CodeEditor(
        state = state,
        fontSize = fontSize,
        fontFamily = fontFamily,
        editable = !readOnly,
        modifier = modifier
    )
}
