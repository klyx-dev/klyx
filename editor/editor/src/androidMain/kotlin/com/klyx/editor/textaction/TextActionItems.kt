package com.klyx.editor.textaction

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.icon.TextSelectStart
import com.klyx.editor.KlyxEditor
import com.klyx.icons.ContentCopy
import com.klyx.icons.ContentCut
import com.klyx.icons.ContentPaste
import com.klyx.icons.FormatAlignLeft
import com.klyx.icons.Icons
import com.klyx.icons.SelectAll

@Suppress("UnusedReceiverParameter", "ParamsComparedByRef")
@Composable
internal fun RowScope.TextActionItems(
    editor: KlyxEditor,
    onClick: (String) -> Unit = {}
) {
    IconButton(
        Icons.SelectAll,
        "Select All"
    ) {
        editor.selectAll()
        onClick("selectall")
    }

    IconButton(Icons.ContentCopy, "Copy") {
        editor.copyText()
        editor.setSelection(editor.cursor.rightLine, editor.cursor.rightColumn)
        onClick("copy")
    }

    IconButton(Icons.ContentCut, "Cut") {
        if (editor.cursor.isSelected) {
            editor.cutText()
        }
        onClick("cut")
    }

    IconButton(Icons.ContentPaste, "Paste") {
        editor.pasteText()
        editor.setSelection(editor.cursor.rightLine, editor.cursor.rightColumn)
        onClick("paste")
    }

    IconButton(KlyxIcons.TextSelectStart, "Long Select") {
        editor.beginLongSelect()
        onClick("longselect")
    }

    IconButton(Icons.FormatAlignLeft, "Format Code") {
        if (editor.cursor.isSelected) {
            editor.formatCodeAsync(
                editor.cursor.left(),
                editor.cursor.right()
            )
        } else {
            editor.formatCodeAsync()
        }
        onClick("formatcode")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconButton(
    imageVector: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit = {}
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
        state = rememberTooltipState(),
        tooltip = {
            if (contentDescription != null) {
                PlainTooltip {
                    Text(contentDescription)
                }
            }
        }
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.padding(11.dp)
            )
        }
    }
}
