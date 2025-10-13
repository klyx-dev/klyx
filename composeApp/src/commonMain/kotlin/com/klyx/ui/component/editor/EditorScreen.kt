package com.klyx.ui.component.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.sp
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.command
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.generateId
import com.klyx.core.io.rememberStoragePermissionState
import com.klyx.core.settings.currentEditorSettings
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.compose.CodeEditor
import com.klyx.tab.Tab
import com.klyx.ui.theme.rememberFontFamily
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalCodeEditorApi::class)
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    editorViewModel: EditorViewModel = koinViewModel(),
    klyxViewModel: KlyxViewModel = koinViewModel(),
) {
    val editorSettings = currentEditorSettings
    val keyboardController = LocalSoftwareKeyboardController.current

    val state by editorViewModel.state.collectAsState()
    val openTabs by remember { derivedStateOf { state.openTabs } }
    val activeTabId by remember { derivedStateOf { state.activeTabId } }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { openTabs.size })
    val scope = rememberCoroutineScope()
    val fontFamily = rememberFontFamily(editorSettings.fontFamily)

    val permissionGranted by rememberStoragePermissionState()

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            editorViewModel.openPendingFiles()
        }
    }

    LaunchedEffect(pagerState, openTabs.size) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            openTabs.getOrNull(page)?.let { tab ->
                editorViewModel.setActiveTab(tab.id)
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
            shortcut(keyShortcutOf(ctrl = true, key = Key.W))
            execute { editorViewModel.closeActiveTab() }
        })
    }

    Column(modifier = modifier) {
        if (openTabs.isNotEmpty()) {
            EditorTabRow(
                tabs = openTabs,
                selectedTab = pagerState.currentPage,
                onTabSelected = { page ->
                    keyboardController?.hide()

                    openTabs.getOrNull(page)?.let { tab ->
                        editorViewModel.setActiveTab(tab.id)
                        scope.launch { pagerState.animateScrollToPage(page) }
                    }
                },
                onClose = { index ->
                    keyboardController?.hide()

                    openTabs.getOrNull(index)?.let { tab ->
                        editorViewModel.closeTab(tab.id)
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
                key = { openTabs.getOrNull(it)?.id ?: generateId() },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (val tab = openTabs.getOrNull(page)) {
                    is Tab.FileTab -> {
                        val file = tab.file

                        if (file.path != "/untitled") {
                            LaunchedEffect(Unit) {
                                if (editorViewModel.isPendingFile(file) && !permissionGranted) {
                                    editorViewModel.closeTab(tab.id)
                                    klyxViewModel.showPermissionDialog()
                                    return@LaunchedEffect
                                }
                            }
                        }

                        //key(tab.file.absolutePath) {
                        CodeEditor(
                            modifier = Modifier.fillMaxSize(),
                            state = tab.editorState,
                            fontFamily = fontFamily,
                            fontSize = editorSettings.fontSize.sp,
                            editable = !tab.isInternal,
                            pinLineNumber = editorSettings.pinLineNumbers
                        )
                        //}
                    }

                    is Tab.AnyTab -> tab.content.invoke()
                    else -> {}
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Open a file or project to get started.",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
