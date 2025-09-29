package com.klyx.editor.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.klyx.editor.compose.draw.EditorCanvas
import com.klyx.editor.compose.draw.drawColor

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState = rememberCodeEditorState(),
    editable: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()
    val colorScheme = MaterialTheme.colorScheme

    EditorCanvas(
        state = state,
        modifier = modifier,
        editable = editable
    ) {
        drawColor(colorScheme.background)

        drawText(
            textMeasurer = textMeasurer,
            text = state.text.getLineContent(1),
            style = TextStyle(
                color = colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
