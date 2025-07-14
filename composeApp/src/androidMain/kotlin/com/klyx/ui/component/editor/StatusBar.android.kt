package com.klyx.ui.component.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.klyx.core.language
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.getActiveEditor
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.text.Cursor
import io.github.rosemoe.sora.widget.subscribeAlways
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue

@OptIn(markerClass = [ExperimentalCodeEditorApi::class])
@Composable
actual fun StatusBar(modifier: Modifier) {
    val viewModel: EditorViewModel = koinViewModel()
    val editor = viewModel.getActiveEditor() ?: return

    var positionText by remember(editor) { mutableStateOf("0:0") }

    fun updatePositionText() {
        val cursor = editor.cursor

        positionText = buildString {
            val (line, column) = when (cursor.selectionDirection) {
                Cursor.DIRECTION_LTR -> cursor.rightLine to cursor.rightColumn
                Cursor.DIRECTION_RTL -> cursor.leftLine to cursor.leftColumn
                else -> cursor.leftLine to cursor.leftColumn
            }

            append("${(1 + line)}:${column}")
            if (cursor.isSelected) {
                append(" (")
                val lines = (cursor.rightLine - cursor.leftLine).absoluteValue

                if (lines > 0) {
                    append("${lines + 1} lines, ")
                }

                val chars = cursor.right - cursor.left
                append("$chars character${if (chars > 1) "s" else ""}")
                append(")")
            }
        }
    }

    LaunchedEffect(Unit) {
        updatePositionText()

        editor.subscribeAlways<SelectionChangeEvent> {
            updatePositionText()
        }
    }

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = positionText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(role = Role.Button) {}
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )

        viewModel.getActiveFile()?.let { file ->
            Text(
                text = file.language(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button) {}
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
