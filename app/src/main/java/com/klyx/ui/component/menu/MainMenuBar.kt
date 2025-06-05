package com.klyx.ui.component.menu

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.key.matches
import com.klyx.core.key.parseShortcut
import com.klyx.core.settings.SettingsManager
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.ui.component.AboutDialog

@Composable
fun MainMenuBar(
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = LocalEditorViewModel.current

    var showPopup by remember { mutableStateOf(false) }
    var iconPosition by remember { mutableStateOf(IntOffset.Zero) }

    var showAbout by remember { mutableStateOf(false) }

    val klyxMenuItems = remember {
        listOf(
            MenuItem("About Klyx...") { showAbout = true },
            MenuItem(),
            MenuItem("Open Settings", "Ctrl-,") {
                viewModel.openFile(SettingsManager.settingsFile)
            },
            MenuItem("Open Default Settings") {
                viewModel.openFile(
                    SettingsManager.internalSettingsFile,
                    tabTitle = "Default Settings",
                    isInternal = true
                )
            },
            MenuItem(),
            MenuItem("Quit", "Ctrl-Q") { activity?.finishAffinity() }
        )
    }

    LaunchedEffect(Unit) {
        klyxMenuItems.fastFilter { it.shortcutKey != null }.fastForEach { item ->
            lifecycleOwner.subscribeToEvent<KeyEvent> { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (event.matches(parseShortcut(item.shortcutKey!!).first())) {
                        item.onClick()
                    }
                }
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { showPopup = !showPopup },
            modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                val position = layoutCoordinates.localToWindow(Offset.Zero)
                iconPosition = IntOffset(position.x.toInt() + 20, position.y.toInt())
            }) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = null
            )
        }

        if (showPopup) {
            PopupMenu(
                items = klyxMenuItems,
                position = iconPosition,
                onDismissRequest = { showPopup = false }
            )
        }
    }

    if (showAbout) AboutDialog { showAbout = false }

}
