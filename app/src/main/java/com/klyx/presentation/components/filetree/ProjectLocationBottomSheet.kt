package com.klyx.presentation.components.filetree

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.klyx.R
import com.klyx.app.icons.Cloud
import com.klyx.app.icons.FolderShared
import com.klyx.app.icons.FolderSpecial
import com.klyx.app.icons.Smartphone
import com.klyx.i18n.strings
import com.klyx.ui.theme.uiFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectLocationBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSelectInternalStorage: () -> Unit,
    onSelectAppDirectory: () -> Unit,
    onSelectTerminalHome: () -> Unit,
    onSelectSftp: () -> Unit,
    onSelectSystemPicker: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = strings.openProject,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = uiFontFamily(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
            )

            Surface(
                onClick = onSelectInternalStorage,
                shape = RoundedCornerShape(16.dp),
                color = Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Transparent),
                    supportingContent = {
                        Text(strings.browseAllFoldersDesc)
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Center
                        ) {
                            Icon(
                                Icons.Rounded.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                ) {
                    Text(
                        strings.internalStorage,
                        fontFamily = uiFontFamily(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectAppDirectory,
                shape = RoundedCornerShape(16.dp),
                color = Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Transparent),
                    supportingContent = {
                        Text(strings.appDataDesc)
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Center
                        ) {
                            Icon(
                                Icons.Rounded.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                ) {
                    Text(
                        strings.appDataDirectory,
                        fontFamily = uiFontFamily(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectTerminalHome,
                shape = RoundedCornerShape(16.dp),
                color = Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Transparent),
                    supportingContent = {
                        Text(strings.terminalHomeDesc)
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Center
                        ) {
                            Icon(
                                painterResource(R.drawable.terminal_2_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                ) {
                    Text(
                        strings.terminalHome,
                        fontFamily = uiFontFamily(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectSftp,
                shape = RoundedCornerShape(16.dp),
                color = Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Transparent),
                    supportingContent = {
                        Text(strings.sftpDesc)
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    CircleShape
                                ),
                            contentAlignment = Center
                        ) {
                            Icon(
                                Icons.Rounded.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Text(
                        "SFTP Connection",
                        fontFamily = uiFontFamily(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectSystemPicker,
                shape = RoundedCornerShape(16.dp),
                color = Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Transparent),
                    supportingContent = {
                        Text(strings.safPickerDesc)
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    CircleShape
                                ),
                            contentAlignment = Center
                        ) {
                            Icon(
                                Icons.Rounded.FolderShared,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                ) {
                    Text(
                        strings.systemPicker,
                        fontFamily = uiFontFamily(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
