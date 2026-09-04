package com.klyx.presentation.components.filetree

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klyx.R
import com.klyx.api.data.file.KxFile
import com.klyx.app.icons.Link
import com.klyx.data.file.resolveName
import com.klyx.i18n.strings
import com.klyx.ui.theme.uiFontFamily
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    searchResultCount: Int,
    isSearching: Boolean,
    showFdHint: Boolean,
    searchEventFlow: SharedFlow<KxFile>,
    searchRoots: List<FileNode>,
    onResultClick: (KxFile) -> Unit
) {
    val resultList = remember { mutableStateListOf<KxFile>() }

    LaunchedEffect(query) {
        resultList.clear()
        searchEventFlow.collect { file ->
            resultList.add(file)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = strings.search,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = uiFontFamily(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_files)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedBorderColor = Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            if (showFdHint && isSearching) {
                Text(
                    text = strings.installFdFind,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (resultList.isNotEmpty()) {
                if (searchResultCount > 0) {
                    Text(
                        text = strings.foundFiles(searchResultCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(resultList) { file ->
                        val (relPath, rootName) = file.uri.path?.let { path ->
                            searchRoots.firstNotNullOfOrNull { root ->
                                root.uri.path?.let { rootPath ->
                                    if (path.startsWith(rootPath)) {
                                        Pair(
                                            path.removePrefix(rootPath).trimStart('/'),
                                            root.name
                                        )
                                    } else null
                                }
                            }
                        } ?: Pair(null, null)
                        val displayPath = if (rootName != null && relPath != null) {
                            if (searchRoots.size > 1) "$rootName › $relPath" else relPath
                        } else {
                            relPath
                        }
                        SearchResultItem(
                            file = file,
                            relativePath = displayPath,
                            onClick = { onResultClick(file) }
                        )
                    }
                }
            } else if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Center
                ) {
                    LoadingIndicator()
                }
            } else if (query.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Center
                ) {
                    Text(
                        text = stringResource(R.string.search_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    file: KxFile,
    relativePath: String?,
    onClick: (KxFile) -> Unit
) {
    Surface(
        onClick = { onClick(file) },
        shape = RoundedCornerShape(8.dp),
        color = Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val fileIcon = iconForFile(file)
            Box {
                Icon(
                    painter = fileIcon.painter,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (fileIcon.tint.isSpecified) fileIcon.tint
                    else if (file.isDirectory) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file.isSymlink) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = strings.symlink,
                        modifier = Modifier
                            .size(10.dp)
                            .align(BottomEnd),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.resolveName(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (relativePath != null) {
                    Text(
                        text = relativePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}
