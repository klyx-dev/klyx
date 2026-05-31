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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.R
import com.klyx.presentation.viewmodel.FileTreeViewModel
import com.klyx.ui.animation.LocalReduceMotion
import com.klyx.ui.animation.orSnap
import com.klyx.ui.theme.GoogleSansRounded
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    FileTree(
                        viewModel = viewModel,
                        onNodeClick = onFileClick,
                        onNodeLongClick = onFileLongClick,
                        modifier = Modifier.fillMaxSize()
                    )
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
            }
        )
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
                    headlineContent = {
                        Text(
                            "Internal Storage",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
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
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectAppDirectory,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            "App Data Directory",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
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
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onSelectSystemPicker,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            "System Picker",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text("Select a specific folder using the built-in file manager.")
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
                                Icons.Rounded.FolderShared,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
