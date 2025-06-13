package com.klyx.ui.component.menu

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.documentfile.provider.DocumentFile
import com.blankj.utilcode.util.UriUtils
import com.klyx.core.cmd.Command
import com.klyx.core.cmd.CommandManager
import com.klyx.core.file.DocumentFileWrapper
import com.klyx.core.file.wrapFile
import com.klyx.core.settings.SettingsManager
import com.klyx.core.showShortToast
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.ui.component.AboutDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

@Composable
fun MainMenuBar(
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current
    val viewModel = LocalEditorViewModel.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.openFile(
                if (DocumentFileWrapper.shouldWrap(uri)) {
                    DocumentFileWrapper(DocumentFile.fromSingleUri(context, uri)!!)
                } else UriUtils.uri2File(uri).wrapFile()
            )
        }
    }

    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) {
            val file = if (DocumentFileWrapper.shouldWrap(uri)) {
                DocumentFileWrapper(DocumentFile.fromSingleUri(context, uri)!!)
            } else UriUtils.uri2File(uri).wrapFile()

            val saved = viewModel.saveAs(file)
            if (saved) context.showShortToast("Saved")
        }
    }

    var showAbout by remember { mutableStateOf(false) }
    var showMenuBar by remember { mutableStateOf(false) }

    val menuItems = remember {
        mapOf(
            "Klyx" to listOf(
                MenuItem("About Klyx...") { showAbout = true },
                MenuItem(),
                MenuItem("Open Settings", "Ctrl-,") {
                    viewModel.openFile(SettingsManager.settingsFile.wrapFile())
                },
                MenuItem("Open Default Settings") {
                    viewModel.openFile(
                        SettingsManager.internalSettingsFile.wrapFile(),
                        tabTitle = "Default Settings",
                        isInternal = true
                    )
                },
                MenuItem(),
                MenuItem("Quit", "Ctrl-Q") {
                    activity?.finishAffinity()
                    exitProcess(0)
                }
            ),

            "File" to listOf(
                MenuItem("New File", "Ctrl-N") {
                    //createFile.launch("untitled")
                    viewModel.openFile(File("untitled").wrapFile())
                },
                MenuItem("Open File...", "Ctrl-O") {
                    openFile.launch(arrayOf("*/*"))
                },
                MenuItem(),
                MenuItem("Save", "Ctrl-S") {
                    val file = viewModel.getActiveFile()
                    if (file == null) {
                        context.showShortToast("No active file")
                        return@MenuItem
                    }

                    if (file.path == "untitled") {
                        createFile.launch(file.name)
                    } else {
                        val saved = viewModel.saveCurrent()
                        if (saved) context.showShortToast("Saved")
                    }
                },
                MenuItem("Save As...", "Ctrl-Shift-S") {
                    val file = viewModel.getActiveFile()
                    if (file == null) {
                        context.showShortToast("No active file")
                        return@MenuItem
                    }
                    createFile.launch(file.name)
                },
                MenuItem("Save All", "Ctrl-Alt-S") {
                    val results = viewModel.saveAll()
                    if (results.isEmpty()) {
                        context.showShortToast("No files to save")
                    } else {
                        val failedFiles = results.filter { !it.value }.keys
                        if (failedFiles.isEmpty()) {
                            context.showShortToast("All files saved")
                        } else {
                            context.showShortToast("Failed to save: ${failedFiles.joinToString(", ")}")
                        }
                    }
                },
            ),

            "Help" to listOf(
                MenuItem("Documentation", dismissRequestOnClicked = false) { context.showShortToast("Soon...") },
                MenuItem("Keyboard Shortcuts", dismissRequestOnClicked = false) {
                    uriHandler.openUri("https://github.com/klyx-dev/klyx/blob/main/docs/keyboard-shortcuts.md")
                },
                MenuItem(),
                MenuItem("Report Issue", dismissRequestOnClicked = false) { uriHandler.openUri("https://github.com/klyx-dev/klyx/issues/new") }
            )
        )
    }

    LaunchedEffect(Unit) {
        menuItems.values.map { it }.fastForEach { items ->
            CommandManager.addCommand(
                *items.fastFilter { !it.isDivider }.fastMap {
                    Command(
                        name = it.title,
                        shortcutKey = it.shortcutKey,
                    ) {
                        scope.launch(Dispatchers.Main.immediate) { it.onClick() }
                    }
                }.toTypedArray()
            )
        }
    }

    var iconPosition by remember { mutableStateOf(IntOffset.Zero) }
    var menuItemPositions by remember { mutableStateOf(mapOf<String, IntOffset>()) }

    val menuItem: (() -> Unit) -> Map<String, @Composable () -> Unit> = remember {
        { onDismissRequest ->
            menuItems.mapValues { (rowTitle, items) ->
                @Composable {
                    PopupMenu(
                        items = items,
                        position = menuItemPositions[rowTitle] ?: IntOffset.Zero,
                        onDismissRequest = onDismissRequest
                    )
                }
            }
        }
    }

    var activeMenu: String? by remember { mutableStateOf(null) }

    LaunchedEffect(activeMenu) {
        if (activeMenu == null) {
            delay(1000)
            showMenuBar = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedVisibility(
            visible = activeMenu == null && !showMenuBar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = {
                    activeMenu = menuItems.keys.first()
                    showMenuBar = true
                },
                modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                    val position = layoutCoordinates.localToWindow(Offset.Zero)
                    iconPosition = IntOffset(position.x.toInt() + 20, position.y.toInt() + 15)
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = null
                )
            }
        }

        AnimatedVisibility(
            visible = showMenuBar || activeMenu != null,
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

    if (showAbout) AboutDialog { showAbout = false }
}

@Composable
private fun MenuRow(
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

            Text(
                text = title,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onMenuItemSelected(index, title) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        val position = layoutCoordinates.localToWindow(Offset.Zero)
                        onMenuItemPositioned(title, IntOffset(position.x.toInt() - 10, position.y.toInt() - 10))
                    }
            )
        }
    }
}
