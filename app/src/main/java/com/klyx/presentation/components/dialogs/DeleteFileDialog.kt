package com.klyx.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.fs.FileSystem
import com.klyx.app.icons.DeleteForever
import com.klyx.i18n.strings
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeleteFileDialog(
    file: KxFile,
    useTrash: Boolean,
    onDismiss: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val fileSystem: FileSystem = koinInject()
    val isDir = file.isDirectory
    val typeName = if (isDir) strings.directory else strings.file

    var isProtected by remember(file) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(file) { isProtected = fileSystem.isProtectedPath(file.uri) }

    val actionsEnabled = isProtected == false

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = strings.deleteTypeQuestion(typeName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (useTrash && isProtected == false) {
                        buildAnnotatedString {
                            append(strings.moveToTrashHintPrefix)
                            withStyle(
                                SpanStyle(fontWeight = FontWeight.Bold)
                            ) {
                                append(file.name)
                            }
                            append(if (isDir) strings.deleteConfirmDirSuffix else strings.deleteConfirmFileSuffix)
                            append(strings.moveToTrashHint)
                        }
                    } else {
                        buildAnnotatedString {
                            append(strings.deleteConfirmPrefix)

                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                append(file.name)
                            }
                            append(if (isDir) strings.deleteConfirmDirSuffix else strings.deleteConfirmFileSuffix)

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(strings.cannotBeUndone)
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (isProtected == true) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ProtectedMessage(isDir)
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isProtected == true) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ButtonDefaults.MediumContainerHeight),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(strings.cancel, style = MaterialTheme.typography.titleMedium)
                    }
                } else if (useTrash) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onMoveToTrash,
                            enabled = actionsEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ButtonDefaults.MediumContainerHeight)
                        ) {
                            Text(strings.moveToTrash, style = MaterialTheme.typography.titleMedium)
                        }

                        OutlinedButton(
                            onClick = onDeletePermanently,
                            enabled = actionsEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ButtonDefaults.MediumContainerHeight)
                        ) {
                            Text(
                                text = strings.deletePermanently,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        FilledTonalButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ButtonDefaults.MediumContainerHeight),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(strings.cancel, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = ButtonDefaults.MediumContainerHeight),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(strings.cancel, style = MaterialTheme.typography.titleMedium)
                        }

                        Button(
                            onClick = onDeletePermanently,
                            enabled = actionsEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = ButtonDefaults.MediumContainerHeight),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(strings.delete, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProtectedMessage(isDir: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = if (isDir) strings.protectedSystemDirectory else strings.protectedSystemFile,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
