package com.klyx.ui.page.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.LocalLogBuffer
import com.klyx.core.FpsTracker
import com.klyx.core.cmd.CommandManager
import com.klyx.core.language
import com.klyx.core.theme.ThemeManager
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.filetree.FileTreeViewModel
import com.klyx.tab.Tab
import com.klyx.ui.component.AboutDialog
import com.klyx.ui.component.ThemeSelector
import com.klyx.ui.component.cmd.CommandPalette
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.component.editor.StatusBar
import com.klyx.ui.component.log.LogViewerSheet
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.StatusBarViewModel
import com.klyx.viewmodel.openLogViewer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalCodeEditorApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainPage(
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    editorViewModel: EditorViewModel = koinViewModel(),
    klyxViewModel: KlyxViewModel = koinViewModel(),
    fileTreeViewModel: FileTreeViewModel = koinViewModel(),
    statusBarViewModel: StatusBarViewModel = koinViewModel(),
    currentRoute: String? = null,
    currentTopDestination: String? = null,
) {
    val logBuffer = LocalLogBuffer.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appState by klyxViewModel.appState.collectAsStateWithLifecycle()
    val project by klyxViewModel.openedProject.collectAsStateWithLifecycle()
    val isTabOpen by editorViewModel.isTabOpen.collectAsStateWithLifecycle()
    val activeTab by editorViewModel.activeTab.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MainTopBar(
                isTabOpen = isTabOpen,
                activeTab = activeTab,
                project = project,
                editorViewModel = editorViewModel,
                klyxViewModel = klyxViewModel,
                fileTreeViewModel = fileTreeViewModel,
                onNavigateToRoute = onNavigateToRoute,
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            LaunchedEffect(Unit) {
                launch {
                    editorViewModel.activeFile.collect { file ->
                        statusBarViewModel.setLanguage(file?.language())
                    }
                }
            }

            val activeTab by editorViewModel.activeTab.collectAsState(null)

            LaunchedEffect(activeTab?.id) {
                when (val tab = activeTab) {
                    is Tab.FileTab -> {
//                        tab.editorState.cursor.collectLatest { cursorState ->
//                            statusBarViewModel.setCursorState(cursorState)
//                        }
                    }

                    else -> {
                        statusBarViewModel.setCursorState(null)
                    }
                }
            }

            EditorScreen(
                modifier = Modifier.weight(1f),
                editorViewModel = editorViewModel,
                klyxViewModel = klyxViewModel
            )

            StatusBar(
                modifier = Modifier.fillMaxWidth(),
                viewModel = statusBarViewModel,
                onLogClick = { klyxViewModel.showLogViewer() }
            )

            if (appState.showLogViewer) {
                LogViewerSheet(
                    buffer = logBuffer,
                    onOpenAsTabClick = {
                        editorViewModel.openLogViewer()
                    },
                    onDismissRequest = klyxViewModel::dismissLogViewer
                )
            }

            val klyxMenuState by klyxViewModel.klyxMenuState.collectAsStateWithLifecycle()

            if (klyxMenuState.showAboutDialog) {
                AboutDialog(
                    onDismissRequest = { klyxViewModel.dismissAboutDialog() }
                )
            }

            if (CommandManager.showCommandPalette) {
                CommandPalette(
                    commands = CommandManager.commands,
                    recentlyUsedCommands = CommandManager.recentlyUsedCommands,
                    onDismissRequest = CommandManager::hidePalette
                )
            }

            if (ThemeManager.showThemeSelector) {
                ThemeSelector(
                    onDismissRequest = ThemeManager::hideThemeSelector
                )
            }
        }
    }
}
