package com.klyx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import arrow.core.Either
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
import com.klyx.ui.AdaptiveLayout
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
            windowSizeClass.isWidthAtLeastMediumOrExpanded -> windowDpSize.width * 0.35f
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
    onFileClick: (KxFile, Worktree) -> Unit,
    onDirectoryPicked: (directory: KxFile, drawerState: Either<WorktreeDrawerState, DrawerState>) -> Unit,
    modifier: Modifier = Modifier,
    gesturesEnabled: (DrawerState) -> Boolean = { true },
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val drawerModifier = Modifier
        .fillMaxHeight()
        .imePadding()
        .navigationBarsPadding()

    AdaptiveLayout(
        compactLayout = {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            CompositionLocalProvider(LocalDrawerState provides drawerState.right()) {
                ModalNavigationDrawer(
                    gesturesEnabled = gesturesEnabled(drawerState),
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerState = drawerState,
                            drawerContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            modifier = modifier.width(worktreeDrawerWidth).then(drawerModifier)
                        ) {
                            val directoryPicker = rememberDirectoryPickerLauncher { file ->
                                if (file != null) {
                                    onDirectoryPicked(file.toKxFile(), drawerState.right())
                                }
                            }

                            DrawerContent(
                                project = project,
                                directoryPicker = directoryPicker,
                                onFileClick = onFileClick,
                                scope = scope,
                                onDismissRequest = drawerState::close
                            )
                        }
                    },
                    content = content,
                )
            }
        }
    )
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
