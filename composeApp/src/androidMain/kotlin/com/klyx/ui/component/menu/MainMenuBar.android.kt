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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.documentfile.provider.DocumentFile
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.UriUtils
import com.klyx.activities.TerminalActivity
import com.klyx.core.DocsUrl
import com.klyx.core.Environment
import com.klyx.core.FpsTracker
import com.klyx.core.KeyboardShortcutsUrl
import com.klyx.core.LocalAppSettings
import com.klyx.core.LocalNotifier
import com.klyx.core.ReportIssueUrl
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.command
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.file.KxFile
import com.klyx.core.file.asDocumentFile
import com.klyx.core.file.toKxFile
import com.klyx.core.openActivity
import com.klyx.core.string
import com.klyx.menu.menu
import com.klyx.res.Res.string
import com.klyx.res.default_new_file_name
import com.klyx.res.file_menu_title
import com.klyx.res.file_picker_all_files_mimetype
import com.klyx.res.help_menu_title
import com.klyx.res.klyx_menu_title
import com.klyx.res.label_fps_suffix
import com.klyx.res.menu_item_about_klyx
import com.klyx.res.menu_item_command_palette
import com.klyx.res.menu_item_documentation
import com.klyx.res.menu_item_extensions
import com.klyx.res.menu_item_keyboard_shortcuts
import com.klyx.res.menu_item_new_file
import com.klyx.res.menu_item_open_default_settings
import com.klyx.res.menu_item_open_file
import com.klyx.res.menu_item_open_settings
import com.klyx.res.menu_item_quit
import com.klyx.res.menu_item_report_issue
import com.klyx.res.menu_item_restart_app
import com.klyx.res.menu_item_save
import com.klyx.res.menu_item_save_all
import com.klyx.res.menu_item_save_as
import com.klyx.res.notification_all_files_saved
import com.klyx.res.notification_failed_to_save
import com.klyx.res.notification_no_active_file
import com.klyx.res.notification_no_files_to_save
import com.klyx.res.notification_saved
import com.klyx.res.tab_title_default_settings
import com.klyx.ui.component.AboutDialog
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.openExtensionScreen
import com.klyx.viewmodel.openSettings
import com.klyx.viewmodel.showWelcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private fun s(resource: StringResource, vararg formatArgs: Any) = string(resource, formatArgs)

@Composable
actual fun MainMenuBar(modifier: Modifier) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val settings = LocalAppSettings.current
    val notifier = LocalNotifier.current

    val fpsTracker = remember { FpsTracker() }
    val fps by fpsTracker.fps

    val viewModel = koinViewModel<EditorViewModel>()
    val scope = rememberCoroutineScope()

    val openFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val file = runCatching {
                UriUtils.uri2FileNoCacheCopy(uri)
            }.getOrNull()?.asDocumentFile()
            viewModel.openFile((file ?: DocumentFile.fromSingleUri(context, uri)!!).toKxFile())
        }
    }

    val createFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(stringResource(string.file_picker_all_files_mimetype))
    ) { uri ->
        if (uri != null) {
            val file =
                (runCatching { UriUtils.uri2FileNoCacheCopy(uri) }.getOrNull()?.asDocumentFile()
                    ?: DocumentFile.fromSingleUri(context, uri)!!).toKxFile()

            val saved = viewModel.saveAs(file)
            if (saved) notifier.success(s(string.notification_saved))
        }
    }

    var showAbout by remember { mutableStateOf(false) }
    var showMenuBar by remember { mutableStateOf(false) }

    val menuItems = remember {
        menu {
            group(string.klyx_menu_title) {
                item(string.menu_item_about_klyx) {
                    onClick { showAbout = true }
                }
                divider()
                item(string.menu_item_open_settings) {
                    shortcut(keyShortcutOf(Key.Comma, ctrl = true))
                    onClick { viewModel.openSettings() }
                }
                item(string.menu_item_open_default_settings) {
                    onClick {
                        viewModel.openFile(
                            KxFile(Environment.InternalSettingsFilePath),
                            tabTitle = s(string.tab_title_default_settings),
                            isInternal = true
                        )
                    }
                }
                divider()
                item("Terminal") {
                    onClick {
                        with(context) {
                            openActivity(TerminalActivity::class)
                        }
                    }
                }
                item(string.menu_item_command_palette) {
                    shortcut(keyShortcutOf(Key.P, ctrl = true, shift = true))
                    onClick { CommandManager.showPalette() }
                }
                item(string.menu_item_extensions) {
                    shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.X))
                    onClick { viewModel.openExtensionScreen() }
                }
                divider()
                "Clear Old Logs" {
                    if (FileUtils.deleteAllInDir(Environment.LogsDir)) {
                        notifier.success("All logs cleared.")
                    } else {
                        notifier.error("Failed to clear logs.")
                    }
                }
                divider()
                item(string.menu_item_restart_app) {
                    onClick { AppUtils.relaunchApp(true) }
                }
                item(string.menu_item_quit) {
                    shortcut(keyShortcutOf(ctrl = true, key = Key.Q))
                    onClick {
                        activity?.finishAffinity()
                        sendSignal(myPid(), SIGNAL_KILL)
                    }
                }
            }

            group(string.file_menu_title) {
                item(string.menu_item_new_file) {
                    shortcut(keyShortcutOf(ctrl = true, key = Key.N))
                    onClick {
                        //createFile.launch("untitled")
                        viewModel.openFile(KxFile(s(string.default_new_file_name)))
                    }
                }
                item(string.menu_item_open_file) {
                    shortcut(keyShortcutOf(ctrl = true, key = Key.O))
                    onClick {
                        openFile.launch(arrayOf(s(string.file_picker_all_files_mimetype)))
                    }
                }
                divider()
                item(string.menu_item_save) {
                    shortcut(keyShortcutOf(ctrl = true, key = Key.S))

                    onClick {
                        val file = viewModel.getActiveFile()
                        if (file == null) {
                            notifier.notify(s(string.notification_no_active_file))
                            return@onClick
                        }

                        if (file.path == "/" + s(string.default_new_file_name)) {
                            createFile.launch(file.name)
                        } else {
                            val saved = viewModel.saveCurrent()
                            if (saved) notifier.toast(s(string.notification_saved))
                        }
                    }
                }
                item(string.menu_item_save_as) {
                    shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.S))

                    onClick {
                        val file = viewModel.getActiveFile()
                        if (file == null) {
                            notifier.notify(s(string.notification_no_active_file))
                            return@onClick
                        }
                        createFile.launch(file.name)
                    }
                }
                item(string.menu_item_save_all) {
                    shortcut(keyShortcutOf(ctrl = true, alt = true, key = Key.S))

                    onClick {
                        val results = viewModel.saveAll()
                        if (results.isEmpty()) {
                            notifier.notify(s(string.notification_no_files_to_save))
                        } else {
                            val failedFiles = results.filter { !it.value }.keys
                            if (failedFiles.isEmpty()) {
                                notifier.toast(s(string.notification_all_files_saved))
                            } else {
                                notifier.error(
                                    s(
                                        string.notification_failed_to_save,
                                        failedFiles.joinToString(", ")
                                    )
                                )
                            }
                        }
                    }
                }
            }

            group(string.help_menu_title) {
                item(string.menu_item_documentation) {
                    onClick {
                        uriHandler.openUri(DocsUrl)
                    }
                }
                item(string.menu_item_keyboard_shortcuts) {
                    onClick {
                        uriHandler.openUri(KeyboardShortcutsUrl)
                    }
                }
                "Show Welcome" {
                    viewModel.showWelcome()
                }
                divider()
                item(string.menu_item_report_issue) {
                    onClick {
                        uriHandler.openUri(ReportIssueUrl)
                    }
                }
            }
        }
    }

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

    var iconPosition by remember { mutableStateOf(IntOffset.Zero) }
    var menuItemPositions by remember { mutableStateOf(mapOf<String, IntOffset>()) }

    val menuItem: (() -> Unit) -> Map<String, @Composable () -> Unit> = remember {
        { onDismissRequest ->
            menuItems.mapValues { (rowTitle, items) ->
                @Composable {
                    PopupMenu(
                        items = items,
                        position = menuItemPositions[rowTitle] ?: IntOffset.Zero,
                        onDismissRequest = onDismissRequest,
                        modifier = Modifier.statusBarsPadding()
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
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        activeMenu = menuItems.keys.first()
                        showMenuBar = true
                    }
                }

                IconButton(
                    onClick = {
                        activeMenu = menuItems.keys.first()
                        showMenuBar = true
                    },
                    interactionSource = interactionSource,
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
                        text = "${"%.2f".format(fps)}${stringResource(string.label_fps_suffix)}",
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
