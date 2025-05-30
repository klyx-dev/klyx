package com.klyx.ui.component.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.klyx.core.file.id
import com.klyx.core.rememberTypeface
import com.klyx.core.settings.EditorSettings
import com.klyx.editor.KlyxCodeEditor
import com.klyx.editor.compose.LocalEditorStore
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.editor.scopeName

@Composable
fun EditorScreen(
    settings: EditorSettings,
    modifier: Modifier = Modifier
) {
    val editors = LocalEditorStore.current
    val context = LocalContext.current
    val viewModel = LocalEditorViewModel.current

    val typefaceText by rememberTypeface(settings.fontFamily)
    val typefaceLineNumber by rememberTypeface(settings.lineNumberFontFamily)

    LaunchedEffect(settings) {
        editors.values.forEach { editor ->
            editor.setTheme(settings.theme)
            editor.isCursorAnimationEnabled = settings.cursorAnimation
            editor.typefaceText = typefaceText
            editor.typefaceLineNumber = typefaceLineNumber
        }
    }

    var currentFileIndex by remember { mutableIntStateOf(0) }

    val state by viewModel.state.collectAsState()
    val openFiles by remember { derivedStateOf { state.openFiles } }

    Column(modifier = modifier) {
        if (openFiles.isNotEmpty()) {
            // Tab bar
            EditorTabBar(
                tabs = openFiles.map { it.name },
                selectedTab = currentFileIndex,
                onTabSelected = { index ->
                    currentFileIndex = index
                    viewModel.setActiveFile(openFiles[index].id)
                },
                onClose = { index ->
                    viewModel.closeFile(openFiles[index].id)
                    editors.remove(openFiles[index].id)
                    if (index == currentFileIndex && openFiles.size > 1) {
                        currentFileIndex = maxOf(0, index - 1)
                        viewModel.setActiveFile(openFiles[currentFileIndex].id)
                    }
                },
                isDirty = { false }
            )

            // Code Editor Area
            val file = openFiles.getOrNull(currentFileIndex)
            val fileId = file?.id

            if (file != null && fileId != null) {
                val editor = editors.getOrPut(fileId) {
                    KlyxCodeEditor(context).apply {
                        setText(file.readText())
                        setLanguage(file.scopeName())
                        setTheme(settings.theme)
                        isCursorAnimationEnabled = settings.cursorAnimation
                    }
                }

                LaunchedEffect(typefaceText, typefaceLineNumber) {
                    editor.typefaceText = typefaceText
                    editor.typefaceLineNumber = typefaceLineNumber
                }

                key(fileId) {
                    AndroidView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .imePadding(),
                        factory = { editor },
                        update = KlyxCodeEditor::requestFocus
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No files open",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
