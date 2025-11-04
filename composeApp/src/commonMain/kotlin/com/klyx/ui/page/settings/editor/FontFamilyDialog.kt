package com.klyx.ui.page.settings.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.core.file.toKxFile
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
import com.klyx.ui.theme.resolveFontFamily
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.stringResource

private fun EditorSettings.updateFontFamily(fontFamily: EditorFontFamily) {
    update { it.copy(fontFamily = fontFamily, useCustomFont = false) }
}

@Composable
fun FontFamilyDialog(
    settings: EditorSettings,
    onDismissRequest: () -> Unit
) {
    var customFontPath by remember { mutableStateOf(settings.customFontPath) }
    var isCustomFontFamily by remember { mutableStateOf(customFontPath != null && settings.useCustomFont) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton {
                if (customFontPath == null) {
                    settings.update { it.copy(useCustomFont = false) }
                }
                onDismissRequest()
            }
        },
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
                                isCustomFontFamily = false
                                settings.updateFontFamily(fontFamily)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.fontFamily == fontFamily && !isCustomFontFamily,
                            onClick = {
                                isCustomFontFamily = false
                                settings.updateFontFamily(fontFamily)
                            }
                        )

                        Text(fontFamily.name, fontFamily = fontFamily.resolveFontFamily())
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                isCustomFontFamily = true
                                settings.update { it.copy(useCustomFont = true) }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isCustomFontFamily,
                            onClick = {
                                isCustomFontFamily = true
                                settings.update { it.copy(useCustomFont = true) }
                            }
                        )

                        Text("Custom", fontFamily = settings.customFontPath?.toKxFile()?.resolveFontFamily())
                    }

                    Column {
                        AnimatedVisibility(isCustomFontFamily) {
                            val fontPicker = rememberFilePickerLauncher(
                                type = FileKitType.File(setOf("ttf", "otf"))
                            ) { file ->
                                if (file != null) {
                                    val kx = file.toKxFile()
                                    customFontPath = kx.absolutePath
                                    settings.update { it.copy(customFontPath = kx.absolutePath, useCustomFont = true) }
                                }
                            }

                            OutlinedTextField(
                                label = { Text("Custom Font Path") },
                                value = customFontPath.orEmpty(),
                                onValueChange = { customFontPath = it },
                                shape = MaterialTheme.shapes.medium,
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                placeholder = { Text("Choose a font") },
                                trailingIcon = {
                                    IconButton(onClick = fontPicker::launch) {
                                        Icon(Icons.Outlined.Folder, contentDescription = "Choose font")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
