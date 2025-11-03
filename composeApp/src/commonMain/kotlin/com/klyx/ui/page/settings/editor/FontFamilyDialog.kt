package com.klyx.ui.page.settings.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.core.icon.BrandFamily
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.settings.EditorFontFamily
import com.klyx.core.settings.EditorSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton
import com.klyx.res.Res
import com.klyx.res.font_family
import com.klyx.ui.component.editor.resolveFontFamily
import org.jetbrains.compose.resources.stringResource

@Composable
fun FontFamilyDialog(
    settings: EditorSettings,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { ConfirmButton(onClick = onDismissRequest) },
        dismissButton = { DismissButton(onClick = onDismissRequest) },
        icon = { Icon(KlyxIcons.BrandFamily, contentDescription = null) },
        title = { Text(stringResource(Res.string.font_family), textAlign = TextAlign.Center) },
        text = {
            LazyColumn {
                stickyHeader {
                    Column {
                        Text(
                            "Choose a font to use for rendering text in the editor.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(EditorFontFamily.entries) { fontFamily ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                settings.update { it.copy(fontFamily = fontFamily) }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.fontFamily == fontFamily,
                            onClick = {
                                settings.update { it.copy(fontFamily = fontFamily) }
                            }
                        )

                        Text(fontFamily.name, fontFamily = fontFamily.resolveFontFamily())
                    }
                }
            }
        }
    )
}
