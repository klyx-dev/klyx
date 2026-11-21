package com.klyx.ui.palette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.lsp.Location
import com.klyx.lsp.fileForNavigationUri
import com.klyx.ui.components.KxDialog
import com.klyx.ui.theme.bodySmall
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.typography
import io.github.vinceglb.filekit.path

@Composable
internal fun ReferenceDialog(
    locations: List<Location>,
    dismiss: () -> Unit,
    select: (Location) -> Unit
) {
    KxDialog(
        visible = true,
        onDismissRequest = dismiss,
        title = "References (${locations.size})"
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            items(locations) { location ->
                val path = remember(location.uri) { fileForNavigationUri(location.uri)?.path ?: location.uri }
                (val line, val character) = location.range.start

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { select(location) }
                        .padding(8.dp)
                ) {
                    val text = "${path.substringAfterLast('/').substringAfterLast('\\')}:${line + 1u}:${character + 1u}"
                    Text(
                        text = text,
                        color = Theme[colors][textPrimary]
                    )

                    Text(path, color = Theme[colors][textMuted], style = Theme[typography][bodySmall])
                }
            }
        }
    }
}
