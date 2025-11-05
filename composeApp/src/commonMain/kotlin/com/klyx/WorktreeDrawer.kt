package com.klyx

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.core.noLocalProvidedFor
import com.klyx.core.ui.component.TextButtonWithShortcut
import com.klyx.extension.api.Project
import com.klyx.extension.api.Worktree
import com.klyx.filetree.FileTree
import com.klyx.filetree.toFileTreeNodes
import com.klyx.res.Res
import com.klyx.res.open_a_project
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

val worktreeDrawerWidth: Dp
    @ReadOnlyComposable
    @Composable
    get() {
        val windowDpSize = LocalWindowInfo.current.containerDpSize
        val windowSizeClass = LocalWindowSizeClass.current
        return when {
            windowSizeClass.isWidthAtLeastMediumOrExpanded -> windowDpSize.width * 0.3f
            else -> windowDpSize.width * 0.7f
        }
    }

val LocalDrawerState = staticCompositionLocalOf<Either<WorktreeDrawerState, DrawerState>> {
    noLocalProvidedFor("LocalDrawerState")
}

inline val isModalNavigationDrawerAvailable
    @Composable
    @ReadOnlyComposable
    get() = LocalDrawerState.current.isRight()

suspend fun Either<WorktreeDrawerState, DrawerState>.openIfClosed() {
    fold(
        ifLeft = { if (it.isClosed) it.open() },
        ifRight = { if (it.isClosed) it.open() }
    )
}

@Composable
fun WorktreeDrawer(
    project: Project,
    drawerState: DrawerState,
    onFileClick: (KxFile, Worktree) -> Unit,
    onDirectoryPicked: (directory: KxFile) -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: suspend () -> Unit = { drawerState.close() },
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val scope = rememberCoroutineScope()

    val directoryPicker = rememberDirectoryPickerLauncher { file ->
        if (file != null) {
            onDirectoryPicked(file.toKxFile())
        }
    }

    val drawerModifier = Modifier
        .fillMaxHeight()
        .imePadding()
        .systemBarsPadding()

    AnimatedContent(targetState = windowSizeClass) { targetClass ->
        when {
            targetClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
                var isExpanded by rememberSaveable { mutableStateOf(true) }
                val width by animateDpAsState(if (isExpanded) worktreeDrawerWidth else 60.dp)

                val drawerState = rememberWorktreeDrawerState(
                    initialValue = if (isExpanded) DrawerValue.Open else DrawerValue.Closed,
                    onOpen = { isExpanded = true },
                    onClose = { isExpanded = false }
                )

                CompositionLocalProvider(LocalDrawerState provides drawerState.left()) {
                    PermanentNavigationDrawer(
                        drawerContent = {
                            PermanentDrawerSheet(
                                drawerShape = RoundedCornerShape(topEnd = 16.dp),
                                drawerContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                modifier = modifier.width(width).then(drawerModifier)
                            ) {
                                Row {
                                    NavigationRail(modifier = Modifier.width(60.dp)) {
                                        NavigationRailItem(
                                            icon = {
                                                Icon(
                                                    if (isExpanded) {
                                                        Icons.Default.Folder
                                                    } else {
                                                        Icons.AutoMirrored.Outlined.DriveFileMove
                                                    },
                                                    contentDescription = if (isExpanded) {
                                                        "Hide worktree"
                                                    } else {
                                                        "Show worktree"
                                                    },
                                                )
                                            },
                                            selected = isExpanded,
                                            onClick = { isExpanded = !isExpanded },
                                        )
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        DrawerContent(project, directoryPicker, onFileClick, scope, onDismissRequest)
                                    }
                                }
                            }
                        },
                        content = content,
                    )
                }
            }

            else -> {
                CompositionLocalProvider(LocalDrawerState provides drawerState.right()) {
                    ModalNavigationDrawer(
                        gesturesEnabled = gesturesEnabled,
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerState = drawerState,
                                drawerContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                modifier = modifier.width(worktreeDrawerWidth).then(drawerModifier)
                            ) {
                                DrawerContent(project, directoryPicker, onFileClick, scope, onDismissRequest)
                            }
                        },
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    project: Project,
    directoryPicker: PickerResultLauncher,
    onFileClick: (KxFile, Worktree) -> Unit,
    scope: CoroutineScope,
    onDismissRequest: suspend () -> Unit
) {
    if (project.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButtonWithShortcut(
                text = stringResource(Res.string.open_a_project),
                modifier = Modifier.padding(top = 20.dp),
                shortcut = keyShortcutOf(ctrl = true, key = Key.O)
            ) {
                directoryPicker.launch()
            }
        }
    } else {
        FileTree(
            rootNodes = project.toFileTreeNodes(),
            modifier = Modifier.fillMaxSize(),
            onFileClick = { file, worktree ->
                onFileClick(file, worktree)
                scope.launch { onDismissRequest() }
            }
        )
    }
}
