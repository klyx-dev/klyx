package com.klyx.ui.component.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.klyx.core.rememberTypeface
import com.klyx.editor.compose.EditorState
import com.klyx.editor.compose.KlyxCodeEditor
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.editor.language.language
import com.klyx.viewmodel.isFileTab
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel = LocalEditorViewModel.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val state by viewModel.state.collectAsState()
    val openTabs by remember { derivedStateOf { state.openTabs } }
    val activeTabId by remember { derivedStateOf { state.activeTabId } }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { openTabs.size })
    val scope = rememberCoroutineScope()
    val typeface by rememberTypeface("IBM Plex Mono")

    val editorStates = remember { mutableStateMapOf<String, EditorState>() }

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

    LaunchedEffect(openTabs) {
        val currentFileIds = openTabs
            .filter { it.type == "file" }
            .map { it.id }
            .toSet()
        editorStates.keys.retainAll { it in currentFileIds }
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
                        if (tab.isFileTab) {
                            editorStates.remove(tab.id)
                        }
                    }
                },
                isDirty = { index ->
                    val tab = openTabs.getOrNull(index)
                    if (tab?.isFileTab == true) {
                        editorStates[tab.id]?.isModified ?: false
                    } else {
                        false
                    }
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
                    tab?.isFileTab == true -> {
                        val file = tab.data as? File
                        if (file != null) {
                            val editorState = editorStates.getOrPut(tab.id) {
                                EditorState(initialText = file.readText())
                            }

                            KlyxCodeEditor(
                                editorState = editorState,
                                typeface = typeface,
                                editable = tab.type != "fileInternal",
                                language = file.language(),
                                onTextChanged = { newText ->
                                    //editorState.updateText(newText)
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Invalid file")
                            }
                        }
                    }

                    else -> {
                        tab?.content?.invoke() ?: run {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${tab?.type?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } ?: "Unknown"} Tab: ${tab?.name ?: ""}")
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
}
