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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.klyx.core.compose.LocalAppSettings
import com.klyx.core.file.id
import com.klyx.core.rememberTypeface
import com.klyx.editor.KlyxCodeEditor
import com.klyx.editor.compose.LocalEditorStore
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.editor.scopeName
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val appSettings = LocalAppSettings.current
    val settings by remember { derivedStateOf { appSettings.editor } }

    val editors = LocalEditorStore.current
    val context = LocalContext.current
    val viewModel = LocalEditorViewModel.current

    val typefaceText by rememberTypeface(settings.fontFamily)
    val typefaceLineNumber by rememberTypeface(settings.lineNumberFontFamily)

    val state by viewModel.state.collectAsState()
    val openFiles by remember { derivedStateOf { state.openFiles } }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { openFiles.size })
    val scope = rememberCoroutineScope()

    // set active file when page changes
    LaunchedEffect(pagerState.currentPage, openFiles.size) {
        openFiles.getOrNull(pagerState.currentPage)?.let { file ->
            viewModel.setActiveFile(file.id)
        }
    }

    // update all editors on settings change
    LaunchedEffect(settings, typefaceText, typefaceLineNumber) {
        editors.values.forEach { editor ->
            editor.setTheme(settings.theme)
            editor.isCursorAnimationEnabled = settings.cursorAnimation
            editor.typefaceText = typefaceText
            editor.typefaceLineNumber = typefaceLineNumber
            editor.setTextSize(settings.fontSize)
        }
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
                    viewModel.closeFile(openFiles[index].id)
                    editors.remove(openFiles[index].id)
                },
                isDirty = { false }
            )

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) { page ->
                val file = openFiles.getOrNull(page)
                val fileId = file?.id

                if (file != null && fileId != null) {
                    val editor = remember(fileId) {
                        editors.getOrPut(fileId) {
                            KlyxCodeEditor(context).apply {
                                setLanguage(file.scopeName())
                                setTheme(settings.theme)
                                setText(file.readText())
                                isCursorAnimationEnabled = settings.cursorAnimation
                            }
                        }
                    }

                    // update fonts only once for this editor
                    LaunchedEffect(typefaceText, typefaceLineNumber) {
                        editor.typefaceText = typefaceText
                        editor.typefaceLineNumber = typefaceLineNumber
                    }

                    LaunchedEffect(editor) {
                        editor.requestFocus()
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { editor },
                        onRelease = KlyxCodeEditor::release
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
