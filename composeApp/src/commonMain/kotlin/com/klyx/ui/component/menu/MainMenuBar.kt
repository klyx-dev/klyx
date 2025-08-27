package com.klyx.ui.component.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.LocalDrawerState
import com.klyx.core.FpsTracker
import com.klyx.core.LocalAppSettings
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.command
import com.klyx.core.toFixed
import com.klyx.filetree.FileTreeViewModel
import com.klyx.menu.rememberMenuItems
import com.klyx.platform.PlatformInfo
import com.klyx.res.Res
import com.klyx.res.label_fps
import com.klyx.ui.component.AboutDialog
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainMenuBar(
    modifier: Modifier = Modifier
) {
    val settings = LocalAppSettings.current

    val editorViewModel = koinViewModel<EditorViewModel>()
    val klyxViewModel = koinViewModel<KlyxViewModel>()
    val fileTreeViewModel = koinViewModel<FileTreeViewModel>()

    val fpsTracker = remember { FpsTracker() }
    val fps by fpsTracker.fps

    val scope = rememberCoroutineScope()

    val menuItems = rememberMenuItems(editorViewModel, klyxViewModel, fileTreeViewModel)

    LaunchedEffect(Unit) {
        menuItems.values.map { it }.fastForEach { items ->
            CommandManager.addCommand(
                items.fastFilter { !it.isDivider }.fastMap {
                    command {
                        name(it.title)
                        shortcut(it.shortcuts)
                        execute {
                            scope.launch(Dispatchers.Main.immediate) { it.onClick() }
                        }
                    }
                }.toTypedArray()
            )
        }
    }

    var menuItemPositions by remember { mutableStateOf(mapOf<String, IntOffset>()) }

    val menuItem: (() -> Unit) -> Map<String, @Composable () -> Unit> = remember {
        { onDismissRequest ->
            menuItems.mapValues { (rowTitle, items) ->
                val position = menuItemPositions[rowTitle] ?: IntOffset.Zero

                @Composable {
                    PopupMenu(
                        items = items,
                        position = position + IntOffset(0, if (PlatformInfo.isDesktop) 30 else 0),
                        onDismissRequest = onDismissRequest
                    )
                }
            }
        }
    }

    var activeMenu: String? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        fpsTracker.start()
    }

    val drawerState = LocalDrawerState.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = null
                )
            }

            if (settings.showFps) {
                Text(
                    text = stringResource(Res.string.label_fps, fps.toFixed(1)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val items = menuItems.map { it.key }
            MenuRow(
                menuItems = items,
                selectedMenuIndex = items.indexOf(activeMenu),
                onMenuItemSelected = { _, title ->
                    activeMenu = title
                },
                onMenuItemPositioned = { title, position ->
                    menuItemPositions = menuItemPositions + (title to position)
                }
            )
        }

        activeMenu?.let {
            menuItem { activeMenu = null }[it]?.invoke()
        }
    }

    val klyxMenuState by klyxViewModel.klyxMenuState.collectAsStateWithLifecycle()

    if (klyxMenuState.showAboutDialog) {
        AboutDialog(
            onDismissRequest = { klyxViewModel.dismissAboutDialog() }
        )
    }
}

@Composable
internal fun MenuRow(
    menuItems: List<String>,
    selectedMenuIndex: Int,
    onMenuItemSelected: (Int, String) -> Unit,
    onMenuItemPositioned: (String, IntOffset) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        menuItems.forEachIndexed { index, title ->
            val isSelected = index == selectedMenuIndex

            val textColor = if (isSelected) {
                colorScheme.primary
            } else {
                colorScheme.onSurface
            }

            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            LaunchedEffect(isHovered) {
                if (isHovered) {
                    onMenuItemSelected(index, title)
                }
            }

            Text(
                text = title,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple()
                    ) {
                        onMenuItemSelected(index, title)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        val position = layoutCoordinates.positionInWindow()
                        onMenuItemPositioned(
                            title,
                            IntOffset(position.x.toInt(), position.y.toInt())
                        )
                    }
            )
        }
    }
}
