package com.klyx.ui.component.menu

import android.os.Process.SIGNAL_KILL
import android.os.Process.myPid
import android.os.Process.sendSignal
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.klyx.core.Environment
import com.klyx.core.FpsTracker
import com.klyx.core.LocalAppSettings
import com.klyx.core.Notifier
import com.klyx.core.cmd.Command
import com.klyx.core.cmd.CommandManager
import com.klyx.core.file.KxFile
import com.klyx.core.file.asDocumentFile
import com.klyx.core.file.toKxFile
import com.klyx.ui.component.AboutDialog
import com.klyx.ui.component.extension.ExtensionScreen
import com.klyx.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun MainMenuBar(modifier: Modifier) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val settings = LocalAppSettings.current

    val fpsTracker = remember { FpsTracker() }
    val fps by fpsTracker.fps

    val viewModel = koinViewModel<EditorViewModel>()
    val notifier: Notifier = koinInject()
    val scope = rememberCoroutineScope()

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val file = runCatching { UriUtils.uri2FileNoCacheCopy(uri) }.getOrNull()?.asDocumentFile()
            viewModel.openFile((file ?: DocumentFile.fromSingleUri(context, uri)!!).toKxFile())
        }
    }

    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) {
            val file = (runCatching { UriUtils.uri2FileNoCacheCopy(uri) }.getOrNull()?.asDocumentFile()
                ?: DocumentFile.fromSingleUri(context, uri)!!).toKxFile()

            val saved = viewModel.saveAs(file)
            if (saved) notifier.notify("Saved")
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
                    viewModel.openFile(KxFile(Environment.SettingsFilePath))
                },
                MenuItem("Open Default Settings") {
                    viewModel.openFile(
                        KxFile(Environment.InternalSettingsFilePath),
                        tabTitle = "Default Settings",
                        isInternal = true
                    )
                },
                MenuItem(),
                MenuItem("Command Palette", "Ctrl-Shift-P") {
                    CommandManager.showPalette()
                },
                MenuItem("Extensions", "Ctrl-Shift-X") {
                    val id = "extension"

                    if (viewModel.isTabOpen(id)) {
                        viewModel.setActiveTab(id)
                    } else {
                        viewModel.openTab("Extensions", id = id) {
                            ExtensionScreen(modifier = Modifier.fillMaxSize())
                        }
                        viewModel.setActiveTab(id)
                    }
                },
//                MenuItem("Terminal") {
//                    val id = "terminal"
//
//                    if (viewModel.isTabOpen(id)) {
//                        viewModel.setActiveTab(id)
//                    } else {
//                        viewModel.openTab("Terminal", id = id) {
//                            TerminalScreen(modifier = Modifier.fillMaxSize())
//                        }
//                        viewModel.setActiveTab(id)
//                    }
//                },
                MenuItem(),
                MenuItem("Quit", "Ctrl-Q") {
                    activity?.finishAffinity()
                    sendSignal(myPid(), SIGNAL_KILL)
                }
            ),

            "File" to listOf(
                MenuItem("New File", "Ctrl-N") {
                    //createFile.launch("untitled")
                    viewModel.openFile(KxFile("untitled"))
                },
                MenuItem("Open File...", "Ctrl-O") {
                    openFile.launch(arrayOf("*/*"))
                },
                MenuItem(),
                MenuItem("Save", "Ctrl-S") {
                    val file = viewModel.getActiveFile()
                    if (file == null) {
                        notifier.notify("No active file")
                        return@MenuItem
                    }

                    if (file.path == "untitled") {
                        createFile.launch(file.name)
                    } else {
                        val saved = viewModel.saveCurrent()
                        if (saved) notifier.notify("Saved")
                    }
                },
                MenuItem("Save As...", "Ctrl-Shift-S") {
                    val file = viewModel.getActiveFile()
                    if (file == null) {
                        notifier.notify("No active file")
                        return@MenuItem
                    }
                    createFile.launch(file.name)
                },
                MenuItem("Save All", "Ctrl-Alt-S") {
                    val results = viewModel.saveAll()
                    if (results.isEmpty()) {
                        notifier.notify("No files to save")
                    } else {
                        val failedFiles = results.filter { !it.value }.keys
                        if (failedFiles.isEmpty()) {
                            notifier.notify("All files saved")
                        } else {
                            notifier.notify("Failed to save: ${failedFiles.joinToString(", ")}")
                        }
                    }
                },
            ),

            "Help" to listOf(
                MenuItem("Documentation") { uriHandler.openUri("https://github.com/klyx-dev/klyx/tree/main/docs") },
                MenuItem("Keyboard Shortcuts") {
                    uriHandler.openUri("https://github.com/klyx-dev/klyx/blob/main/docs/keyboard-shortcuts.md")
                },
                MenuItem(),
                MenuItem("Report Issue") { uriHandler.openUri("https://github.com/klyx-dev/klyx/issues/new") }
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

    LaunchedEffect(Unit) {
        fpsTracker.start()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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

                Spacer(modifier = Modifier.weight(1f))

                if (settings.showFps) {
                    Text(
                        text = "${"%.2f".format(fps)} FPS",
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
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
