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
import com.klyx.core.file.id
import com.klyx.core.rememberTypeface
import com.klyx.editor.compose.EditorState
import com.klyx.editor.compose.KlyxCodeEditor
import com.klyx.editor.compose.LocalEditorStore
import com.klyx.editor.compose.LocalEditorViewModel
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val editors = LocalEditorStore.current
    val viewModel = LocalEditorViewModel.current

    val state by viewModel.state.collectAsState()
    val openFiles by remember { derivedStateOf { state.openFiles } }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { openFiles.size })
    val scope = rememberCoroutineScope()
    val typeface by rememberTypeface("IBM Plex Mono")

    // Store EditorState instances for each open file
    val editorStates = remember { mutableStateMapOf<String, EditorState>() }

    LaunchedEffect(pagerState, openFiles.size) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            openFiles.getOrNull(page)?.let { file ->
                viewModel.setActiveFile(file.id)
            }
        }
    }

    // Clean up editorStates when files are closed
    LaunchedEffect(openFiles) {
        val currentFileIds = openFiles.map { it.id }.toSet()
        editorStates.keys.retainAll { it in currentFileIds }
    }

    Column(modifier = modifier) {
        if (openFiles.isNotEmpty()) {
            EditorTabBar(
                tabs = openFiles.map { it.name },
                selectedTab = pagerState.currentPage,
                onTabSelected = { page ->
                    viewModel.setActiveFile(openFiles[page].id)
                    scope.launch { pagerState.animateScrollToPage(page) }
                },
                onClose = { index ->
                    val fileToCloseId = openFiles[index].id
                    viewModel.closeFile(fileToCloseId)
                    editors.remove(fileToCloseId)
                    // editorStates will be cleaned up by the LaunchedEffect(openFiles)
                },
                isDirty = {
                    // Placeholder: Implement actual dirty checking based on EditorState if needed
                    // val fileId = openFiles.getOrNull(it)?.id
                    // fileId?.let { editorStates[it]?.isModified } ?: false
                    false
                }
            )

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) { page ->
                val file = openFiles.getOrNull(page)

                if (file != null) {
                    // Get or create EditorState for the current file
                    val editorState = editorStates.getOrPut(file.id) {
                        EditorState(initialText = file.readText())
                    }

                    KlyxCodeEditor(
                        editorState = editorState, // Pass the EditorState instance
                        typeface = typeface,
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files open")
            }
        }
    }
}
