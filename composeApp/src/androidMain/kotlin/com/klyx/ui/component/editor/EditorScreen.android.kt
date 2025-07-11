package com.klyx.ui.component.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.klyx.core.LocalAppSettings
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.command
import com.klyx.core.file.KxFile
import com.klyx.core.file.requiresPermission
import com.klyx.core.hasStoragePermission
import com.klyx.core.requestStoragePermission
import com.klyx.editor.CodeEditor
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.tab.Tab
import com.klyx.ui.theme.rememberFontFamily
import com.klyx.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalCodeEditorApi::class)
@Composable
actual fun EditorScreen(modifier: Modifier) {
    val context = LocalContext.current
    val editorSettings = LocalAppSettings.current.editor
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val viewModel = koinViewModel<EditorViewModel>()

    val state by viewModel.state.collectAsState()
    val openTabs by remember { derivedStateOf { state.openTabs } }
    val activeTabId by remember { derivedStateOf { state.activeTabId } }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { openTabs.size })
    val scope = rememberCoroutineScope()
    val fontFamily = rememberFontFamily(editorSettings.fontFamily)

    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingFile: KxFile? by remember { mutableStateOf(null) }
    var permissionGranted by rememberSaveable { mutableStateOf(context.hasStoragePermission()) }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasStoragePermission()
            }
        }

        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)

        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            pendingFile?.let {
                viewModel.openFile(it)
                pendingFile = null
            }
        }
    }

    LaunchedEffect(pagerState, openTabs.size) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            openTabs.getOrNull(page)?.let { tab ->
                viewModel.setActiveTab(tab.id)
            }
        }
    }

    LaunchedEffect(activeTabId, pagerState) {
        val index = openTabs.indexOfFirst { it.id == activeTabId }
        if (index != -1 && index != pagerState.currentPage) {
            scope.launch {
                pagerState.animateScrollToPage(index)
            }
        }
    }

    LaunchedEffect(Unit) {
        CommandManager.addCommand(command {
            name("Close Active Tab")
            shortcutKey("Ctrl-W")
            execute { viewModel.closeActiveTab() }
        })
    }

    Column(modifier = modifier) {
        if (openTabs.isNotEmpty()) {
            EditorTabBar(
                tabs = openTabs,
                selectedTab = pagerState.currentPage,
                onTabSelected = { page ->
                    keyboardController?.hide()

                    openTabs.getOrNull(page)?.let { tab ->
                        viewModel.setActiveTab(tab.id)
                        scope.launch { pagerState.animateScrollToPage(page) }
                    }
                },
                onClose = { index ->
                    openTabs.getOrNull(index)?.let { tab ->
                        viewModel.closeTab(tab.id)
                    }
                },
                isDirty = { index ->
                    val tab = openTabs.getOrNull(index)
                    if (tab is Tab.FileTab) tab.isModified else false
                }
            )

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) { page ->
                val tab = openTabs.getOrNull(page)

                when {
                    tab is Tab.FileTab -> {
                        val file = tab.file

                        if (file.path != "/untitled") {
                            LaunchedEffect(Unit) {
                                if (file.requiresPermission(context, isWrite = true)) {
                                    viewModel.closeTab(tab.id)
                                    pendingFile = file
                                    showPermissionDialog = true
                                    return@LaunchedEffect
                                }
                            }
                        }

                        CodeEditor(
                            modifier = Modifier.fillMaxSize(),
                            state = tab.editorState,
                            fontFamily = fontFamily,
                            fontSize = editorSettings.fontSize.sp,
                            editable = !tab.isInternal,
                            pinLineNumber = editorSettings.pinLineNumbers,
                            language = when (tab.file.extension) {
                                "kt", "kts" -> "kotlin"
                                "js" -> "javascript"
                                "json" -> "json"
                                else -> null
                            }
                        )
                    }

                    else -> {
                        tab?.content?.invoke() ?: run {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Unknown Tab: ${tab?.name ?: ""}")
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tabs open")
            }
        }
    }

    if (showPermissionDialog && !permissionGranted) {
        PermissionDialog(
            onDismissRequest = { showPermissionDialog = false },
            onRequestPermission = context::requestStoragePermission
        )
    }
}
