package com.klyx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.ui.component.TextButtonWithShortcut
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.res.Res
import com.klyx.res.open_a_project
import com.klyx.extension.api.Project
import com.klyx.extension.api.Worktree
import com.klyx.filetree.FileTree
import com.klyx.filetree.toFileTreeNodes
import com.klyx.platform.PlatformInfo
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

val WorktreeDrawerWidth: Dp
    @ReadOnlyComposable
    @Composable
    get() {
        val density = LocalDensity.current
        val windowInfo = LocalWindowInfo.current
        val width = windowInfo.containerSize.width * if (PlatformInfo.isMobile) 0.75f else 0.3f
        return with(density) { width.toDp() }
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
    val scope = rememberCoroutineScope()

    val directoryPicker = rememberDirectoryPickerLauncher { file ->
        if (file != null) {
            onDirectoryPicked(file.toKxFile())
        }
    }

    CompositionLocalProvider(LocalDrawerState provides drawerState) {
        ModalNavigationDrawer(
            gesturesEnabled = gesturesEnabled,
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerState = drawerState,
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    modifier = modifier.width(WorktreeDrawerWidth).fillMaxHeight().imePadding()
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
            },
            content = content,
        )
    }
}
