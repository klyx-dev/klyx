package com.klyx.presentation.components.filetree

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.R
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.fs.Paths
import com.klyx.api.terminal.home
import com.klyx.api.ui.theme.GoogleSansRounded
import com.klyx.data.file.resolveName
import com.klyx.data.fs.SftpFileSystem
import com.klyx.presentation.viewmodel.FileTreeViewModel
import com.klyx.ui.animation.LocalReduceMotion
import com.klyx.ui.animation.orSnap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTreeDrawer(
    viewModel: FileTreeViewModel,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    gesturesEnabled: Boolean = false,
    onFileClick: (node: FileNode, rootNode: FileNode) -> Unit = { _, _ -> },
    onFileLongClick: (node: FileNode, rootNode: FileNode) -> Unit = { _, _ -> },
    screenContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val drawerWidth = FileTree.drawerWidth()

    val fraction by remember {
        derivedStateOf {
            val widthPx = with(density) { drawerWidth.toPx() }
            val offset = drawerState.currentOffset
            (1f + (offset / widthPx)).coerceIn(0f, 1f)
        }
    }

    // Bottom Sheet State
    var showLocationPicker by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded)
    )

    var showSearchSheet by remember { mutableStateOf(false) }
    var showSftpSheet by remember { mutableStateOf(false) }
    val searchSheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, PartiallyExpanded, Expanded)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val directoryPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }.onFailure { it.printStackTrace() }
                val file = DocumentFile.fromTreeUri(context, uri)!!
                viewModel.addRootNode(file.uri)
            }
        }

    ModalNavigationDrawer(
        modifier = modifier,
        gesturesEnabled = gesturesEnabled,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerState = drawerState,
                modifier = Modifier.width(drawerWidth),
                drawerContentColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                val isOpening = drawerState.targetValue == DrawerValue.Open
                val isFullyClosed =
                    drawerState.currentValue == DrawerValue.Closed && drawerState.targetValue == DrawerValue.Closed

                if (uiState.rootNodes.isEmpty()) {
                    EmptyState(
                        isOpening = isOpening,
                        isFullyClosed = isFullyClosed,
                        onOpenProjectClick = { showLocationPicker = true }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { showLocationPicker = true },
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.add_folder),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Surface(
                                onClick = { showSearchSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            ) {
                                Box(
                                    modifier = Modifier.size(42.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        FileTree(
                            viewModel = viewModel,
                            onNodeClick = onFileClick,
                            onNodeLongClick = onFileLongClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        val x = (drawerWidth * fraction).toPx()
                        val extraOffset = -12.dp.toPx() * fraction

                        IntOffset(
                            x = (x + extraOffset).fastRoundToInt(),
                            y = 0
                        )
                    }
            ) {
                screenContent()
            }
        }
    )

    if (showLocationPicker) {
        fun dismiss() {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    showLocationPicker = false
                }
            }
        }

        ProjectLocationBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showLocationPicker = false },
            onSelectSystemPicker = {
                dismiss()
                directoryPicker.launch(null)
            },
            onSelectInternalStorage = {
                dismiss()
                viewModel.addRootNode(Uri.fromFile(Environment.getExternalStorageDirectory()))
            },
            onSelectAppDirectory = {
                dismiss()
                viewModel.addRootNode(Uri.fromFile(context.dataDir))
            },
            onSelectTerminalHome = {
                dismiss()
                viewModel.addRootNode(Uri.fromFile(Paths.home))
            },
            onSelectSftp = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showLocationPicker = false
                        showSftpSheet = true
                    }
                }
            }
        )
    }

    if (showSftpSheet) {
        SftpConnectionSheet(
            onDismiss = { showSftpSheet = false },
            onConnect = { host, port, username, password, path ->
                showSftpSheet = false
                val userPart = if (password != null) "$username:$password" else username
                val uri = Uri.parse("sftp://$userPart@$host:$port$path")
                viewModel.addRootNode(uri)
            }
        )
    }

    if (showSearchSheet) {
        fun dismiss() {
            scope.launch { searchSheetState.hide() }.invokeOnCompletion {
                if (!searchSheetState.isVisible) {
                    showSearchSheet = false
                }
            }
        }

        SearchBottomSheet(
            sheetState = searchSheetState,
            onDismissRequest = { showSearchSheet = false },
            onQueryChange = viewModel::search,
            query = uiState.searchQuery,
            searchResultCount = uiState.searchResultCount,
            isSearching = uiState.isSearching,
            showFdHint = !uiState.hasFastSearch && uiState.rootNodes.any { it.uri.scheme == "file" },
            searchEventFlow = viewModel.searchEventFlow,
            searchRoots = uiState.rootNodes,
            onResultClick = { file ->
                viewModel.selectSearchResult(file)
                scope.launch { drawerState.open() }
                dismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchBottomSheet(
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
                text = "Search",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = GoogleSansRounded,
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
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            if (showFdHint && isSearching) {
                Text(
                    text = "Install fd-find for faster search: apt install fd-find",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (resultList.isNotEmpty()) {
                if (searchResultCount > 0) {
                    Text(
                        text = "Found $searchResultCount files",
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
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            } else if (query.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
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
private fun SearchResultItem(
    file: KxFile,
    relativePath: String?,
    onClick: (KxFile) -> Unit
) {
    Surface(
        onClick = { onClick(file) },
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
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
            Icon(
                painter = fileIcon.painter,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (fileIcon.tint.isSpecified) fileIcon.tint
                else if (file.isDirectory) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyState(
    isOpening: Boolean,
    isFullyClosed: Boolean,
    onOpenProjectClick: () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current

    val heroScale = remember { Animatable(0.5f) }
    val heroAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(40f) }
    val buttonScale = remember { Animatable(0.8f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(isOpening, isFullyClosed) {
        if (isOpening) {
            launch { heroAlpha.animateTo(1f, tween<Float>(300).orSnap(reduceMotion)) }
            launch {
                heroScale.animateTo(
                    1f,
                    spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ).orSnap(reduceMotion)
                )
            }

            delay(100.milliseconds)

            launch { textAlpha.animateTo(1f, tween<Float>(400).orSnap(reduceMotion)) }
            launch {
                textOffsetY.animateTo(
                    0f,
                    spring<Float>(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    ).orSnap(reduceMotion)
                )
            }

            delay(100.milliseconds)

            launch { buttonAlpha.animateTo(1f, tween<Float>(200).orSnap(reduceMotion)) }
            launch {
                buttonScale.animateTo(
                    1f,
                    spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ).orSnap(reduceMotion)
                )
            }
        } else if (isFullyClosed) {
            heroScale.snapTo(0.5f)
            heroAlpha.snapTo(0f)
            textAlpha.snapTo(0f)
            textOffsetY.snapTo(40f)
            buttonScale.snapTo(0.8f)
            buttonAlpha.snapTo(0f)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 20000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotationAngle"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = heroScale.value
                            scaleY = heroScale.value
                            alpha = heroAlpha.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                rotationZ = rotationAngle
                            }
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialShapes.Cookie12Sided.toShape()
                            )
                    )

                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = "Folder Icon",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlpha.value
                        translationY = textOffsetY.value
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ready_to_code),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.open_folder_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 360.dp)
                    )
                }

                //Spacer(modifier = Modifier.height(8.dp))

                OpenProjectButton(
                    onClick = onOpenProjectClick,
                    modifier = Modifier.graphicsLayer {
                        scaleX = buttonScale.value
                        scaleY = buttonScale.value
                        alpha = buttonAlpha.value
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OpenProjectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = ButtonDefaults.MediumContainerHeight

    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.heightIn(buttonHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight, hasStartIcon = true),
    ) {
        Icon(
            painterResource(R.drawable.folder_open_24px),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonHeight)))
        Text(
            text = stringResource(R.string.open_project),
            style = ButtonDefaults.textStyleFor(buttonHeight),
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold
        )
    }
}

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
                text = "Open Project",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
            )

            Surface(
                onClick = onSelectInternalStorage,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    supportingContent = {
                        Text("Browse and access all folders available on your device.")
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
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
                        "Internal Storage",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectAppDirectory,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    supportingContent = {
                        Text("Direct access to the internal data directory where all app files are stored.")
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
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
                        "App Data Directory",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectTerminalHome,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    supportingContent = {
                        Text($$"Open the terminal's home directory ($HOME). This is the default working directory where your files, projects, and personal data are stored inside the terminal environment.")
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
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
                        "Terminal Home",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectSftp,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    supportingContent = {
                        Text("Connect to a remote server via SFTP to browse and edit files.")
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
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
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectSystemPicker,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    supportingContent = {
                        Text("Select a specific folder using the built-in file manager.")
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
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
                        "System Picker",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SftpConnectionSheet(
    onDismiss: () -> Unit,
    onConnect: (host: String, port: Int, username: String, password: String?, path: String) -> Unit
) {
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("/") }
    var detecting by remember { mutableStateOf(false) }
    var detectError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isValid = host.isNotBlank() && username.isNotBlank()

    fun detectPath() {
        detectError = null
        detecting = true
        scope.launch {
            try {
                val home = withContext(Dispatchers.IO) {
                    SftpFileSystem.detectDefaultPath(
                        host,
                        portText.toIntOrNull() ?: 22,
                        username,
                        password.ifBlank { null })
                }
                path = home
            } catch (e: Exception) {
                detectError = e.message ?: "Detection failed"
            } finally {
                detecting = false
            }
        }
    }

    val sheetState = rememberBottomSheetState(initialValue = Expanded, enabledValues = setOf(Expanded, Hidden))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SFTP Connection",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    placeholder = { Text("e.g. 192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") },
                    placeholder = { Text("22") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { Text("e.g. root") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it; detectError = null },
                    label = { Text("Path") },
                    placeholder = { Text("/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    isError = detectError != null,
                    supportingText = if (detectError != null) {
                        { Text(detectError!!, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    trailingIcon = {
                        if (detecting) {
                            LoadingIndicator(
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            TextButton(
                                onClick = { detectPath() },
                                enabled = host.isNotBlank() && username.isNotBlank() && !detecting,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Detect", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    Text("Cancel", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = {
                        val port = portText.toIntOrNull() ?: 22
                        val safePath = path.ifBlank { "/" }.let { if (it.startsWith("/")) it else "/$it" }
                        onConnect(host, port, username, password.ifBlank { null }, safePath)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ButtonDefaults.MediumContainerHeight),
                    enabled = isValid
                ) {
                    Text("Connect", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
