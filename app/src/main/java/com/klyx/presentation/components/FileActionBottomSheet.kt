package com.klyx.presentation.components

import android.content.ClipData
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ContentPasteGo
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klyx.data.editor.icon
import com.klyx.data.file.FileStatInfo
import com.klyx.data.file.KxFile
import com.klyx.data.file.calculateTotalSize
import com.klyx.data.file.resolveName
import com.klyx.data.file.statInfo
import com.klyx.data.file.symlinkTarget
import com.klyx.presentation.components.subcomponents.AutoSizeText
import com.klyx.ui.theme.GoogleSansRounded
import com.klyx.util.asLocalDateTime
import com.klyx.util.formatDateTime
import com.klyx.util.humanBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.time.Duration.Companion.milliseconds

sealed interface FileAction
sealed interface DirectoryAction

data class Rename(val file: KxFile) : FileAction, DirectoryAction
data class Copy(val file: KxFile) : FileAction
data class CopyPath(val file: KxFile) : FileAction, DirectoryAction
data class Cut(val file: KxFile) : FileAction
data class Paste(val destination: KxFile) : FileAction, DirectoryAction
data class Share(val file: KxFile) : FileAction
data class OpenWith(val file: KxFile) : FileAction
data class Delete(val file: KxFile) : FileAction, DirectoryAction
data class NewFile(val parent: KxFile) : DirectoryAction
data class NewDirectory(val parent: KxFile) : DirectoryAction
data class CloseProject(val file: KxFile) : DirectoryAction

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileActionBottomSheet(
    file: KxFile,
    isProject: Boolean,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onFileAction: (FileAction) -> Unit,
    onDirectoryAction: (DirectoryAction) -> Unit
) {
    val evenCornerRadiusElems = 26.dp

    val cornerShape = remember(evenCornerRadiusElems) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = evenCornerRadiusElems,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = evenCornerRadiusElems,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = evenCornerRadiusElems,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = evenCornerRadiusElems,
            smoothnessAsPercentTR = 60
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()

        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(cornerShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painterResource(file.icon()),
                                    contentDescription = "File Icon",
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.size(32.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                AutoSizeText(
                                    text = file.resolveName(),
                                    modifier = Modifier.padding(end = 4.dp),
                                    fontWeight = FontWeight.Light,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Swipeable Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = tween(durationMillis = 280),
                                alignment = Alignment.TopCenter
                            )
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            when (page) {
                                0 -> { // Options / Actions
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (file.isDirectory) {
                                            directoryActions(
                                                file = file,
                                                isProject = isProject,
                                                cornerShape = cornerShape,
                                                onAction = onDirectoryAction
                                            )
                                        } else {
                                            fileActions(file, cornerShape, onFileAction)
                                        }

                                        item {
                                            Spacer(Modifier.height(77.dp))
                                        }
                                    }
                                }

                                1 -> { // Details / Info
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item {
                                            InfoItems(file)
                                        }
                                        item {
                                            Spacer(Modifier.height(80.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Tab Bar
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(5.dp),
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    AnimatedTab(
                        index = 0,
                        selectedIndex = pagerState.currentPage,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Menu,
                                contentDescription = "Options",
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "OPTIONS",
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    AnimatedTab(
                        index = 1,
                        selectedIndex = pagerState.currentPage,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = "Details",
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "INFO",
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItems(
    file: KxFile,
    infoSegmentItemShape: RoundedCornerShape = remember { RoundedCornerShape(8.dp) }
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    fun copyText(text: String) {
        coroutineScope.launch {
            clipboard.setClipEntry(ClipData.newPlainText("klyx", text).toClipEntry())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FileInfoSegmentedListItem(
            headline = "Name",
            supporting = file.resolveName(),
            icon = FileIcon.Drawable(file.icon()),
            iconDescription = "File icon",
            shape = infoSegmentItemShape,
        )

        FileInfoSegmentedListItem(
            headline = "Path",
            supporting = file.path,
            icon = Icons.Rounded.AccountTree.fileIcon,
            iconDescription = "File icon",
            shape = infoSegmentItemShape,
            onClick = { copyText(file.path) },
        )

        var sizeText by remember(file) {
            mutableStateOf(if (file.isDirectory) "Calculating..." else file.length.humanBytes())
        }

        LaunchedEffect(file) {
            if (file.isDirectory) {
                file.calculateTotalSize()
                    .collect { progress ->
                        val sizeStr =
                            progress.bytes.humanBytes()
                        val details =
                            "${progress.fileCount} files, ${progress.dirCount} folders"

                        sizeText =
                            if (progress.isFinished) {
                                "$sizeStr ($details)"
                            } else {
                                "$sizeStr ($details...)"
                            }
                    }
            }
        }

        FileInfoSegmentedListItem(
            headline = "Size",
            supporting = sizeText,
            icon = Icons.Rounded.Storage.fileIcon,
            iconDescription = null,
            shape = infoSegmentItemShape,
        )

        FileInfoSegmentedListItem(
            headline = "Last modified",
            supporting = file.lastModified.milliseconds.asLocalDateTime().formatDateTime(),
            icon = Icons.Rounded.EditCalendar.fileIcon,
            iconDescription = null,
            shape = infoSegmentItemShape,
        )

        val statInfo by produceState<FileStatInfo?>(initialValue = null, file) {
            value = withContext(Dispatchers.IO) { file.statInfo }
        }

        statInfo?.let {
            FileInfoSegmentedListItem(
                headline = "Permissions",
                supporting = statInfo!!.permissions,
                icon = Icons.Rounded.AdminPanelSettings.fileIcon,
                iconDescription = "Permission icon",
                shape = infoSegmentItemShape,
                onClick = { copyText(statInfo!!.permissions) }
            )
        }

        val symlinkTarget by produceState<String?>(initialValue = null, file) {
            value = withContext(Dispatchers.IO) { file.symlinkTarget }
        }

        symlinkTarget?.let { target ->
            FileInfoSegmentedListItem(
                headline = "Symbolic link",
                supporting = "→ $target",
                icon = Icons.Rounded.Link.fileIcon,
                iconDescription = null,
                shape = infoSegmentItemShape,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.directoryActions(
    file: KxFile,
    isProject: Boolean,
    cornerShape: Shape,
    onAction: (DirectoryAction) -> Unit
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier
                    .weight(0.5f)
                    .heightIn(min = 66.dp),
                shape = cornerShape,
                onClick = { onAction(NewFile(file)) }
            ) {
                Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = "New File")
                Spacer(Modifier.width(8.dp))
                Text("New File", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }

            Button(
                modifier = Modifier
                    .weight(0.5f)
                    .heightIn(min = 66.dp),
                shape = cornerShape,
                onClick = { onAction(NewDirectory(file)) }
            ) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = "New Folder")
                Spacer(Modifier.width(8.dp))
                Text("New Folder", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(0.35f)
                    .heightIn(min = 66.dp),
                onClick = { onAction(Rename(file)) }
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Rename")
                Spacer(Modifier.width(8.dp))
                Text("Rename", style = MaterialTheme.typography.titleMedium)
            }

            FilledTonalButton(
                modifier = Modifier
                    .weight(0.5f)
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                onClick = { onAction(CopyPath(file)) }
            ) {
                Icon(Icons.Rounded.CopyAll, contentDescription = "Copy Path")
                Spacer(Modifier.width(8.dp))
                Text("Copy Path", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(0.7f)
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                onClick = { onAction(Paste(file)) }
            ) {
                Icon(Icons.Rounded.ContentPasteGo, contentDescription = "Paste Here")
                Spacer(Modifier.width(8.dp))
                Text("Paste Here", style = MaterialTheme.typography.titleMedium)
            }

            FilledTonalButton(
                modifier = Modifier
                    .weight(0.5f)
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                onClick = { onAction(Delete(file)) }
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete")
                Spacer(Modifier.width(8.dp))
                Text("Delete", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (isProject) {
        item {
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { onAction(CloseProject(file)) }
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ExitToApp,
                    contentDescription = "Close Project"
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Close Project",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.fileActions(
    file: KxFile,
    cornerShape: AbsoluteSmoothCornerShape,
    onAction: (FileAction) -> Unit
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(0.65f)
                    .heightIn(min = 66.dp),
                shape = FloatingActionButtonDefaults.shape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = { onAction(OpenWith(file)) }
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open with")
                Spacer(Modifier.width(8.dp))
                Text(
                    "Open with",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                modifier = Modifier
                    .weight(0.5f)
                    .heightIn(min = 66.dp),
                shape = cornerShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                onClick = { onAction(Rename(file)) }
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Rename")
                Spacer(Modifier.width(8.dp))
                Text("Rename", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(0.33f)
                    .heightIn(min = 66.dp),
                contentPadding = PaddingValues(0.dp),
                onClick = { onAction(Copy(file)) }
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy")
                Spacer(Modifier.width(4.dp))
                Text("Copy", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            FilledTonalButton(
                modifier = Modifier
                    .weight(0.33f)
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = { onAction(Cut(file)) }
            ) {
                Icon(Icons.Rounded.ContentCut, contentDescription = "Cut")
                Spacer(Modifier.width(4.dp))
                Text("Cut", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            FilledTonalButton(
                modifier = Modifier
                    .weight(0.33f)
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = { onAction(Paste(file.parentFile ?: file)) }
            ) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste")
                Spacer(Modifier.width(4.dp))
                Text("Paste", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(0.6f)
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                onClick = { onAction(CopyPath(file)) }
            ) {
                Icon(Icons.Rounded.CopyAll, contentDescription = "Copy Path")
                Spacer(Modifier.width(8.dp))
                Text(
                    "Copy Path",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                modifier = Modifier
                    .weight(0.4f)
                    .heightIn(min = 66.dp),
                onClick = { onAction(Share(file)) }
            ) {
                Icon(Icons.Rounded.Share, contentDescription = "Share")
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    item {
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            onClick = { onAction(Delete(file)) }
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = "Delete")
            Spacer(Modifier.width(8.dp))
            Text("Delete File", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private inline val ImageVector.fileIcon get() = FileIcon.Image(this)

private sealed interface FileIcon {
    data class Image(val imageVector: ImageVector) : FileIcon
    data class Drawable(val drawableId: Int) : FileIcon
}

@Composable
private fun FileInfoSegmentedListItem(
    headline: String,
    supporting: String,
    icon: FileIcon,
    iconDescription: String?,
    shape: Shape,
    onClick: (() -> Unit)? = null,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .let { baseModifier ->
            if (onClick != null) {
                baseModifier.clickable(onClick = onClick)
            } else {
                baseModifier
            }
        }

    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(headline) },
            supportingContent = { Text(supporting) },
            leadingContent = {
                when (icon) {
                    is FileIcon.Drawable -> {
                        Icon(
                            painterResource(icon.drawableId),
                            contentDescription = iconDescription,
                        )
                    }

                    is FileIcon.Image -> {
                        Icon(
                            imageVector = icon.imageVector,
                            contentDescription = iconDescription,
                        )
                    }
                }
            }
        )
    }
}
