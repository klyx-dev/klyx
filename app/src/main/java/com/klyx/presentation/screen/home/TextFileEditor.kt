package com.klyx.presentation.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import com.klyx.data.editor.EditorStateRegistry
import com.klyx.data.editor.KlyxEditorColorScheme
import com.klyx.data.editor.applyEditorSettings
import com.klyx.data.preferences.FontManager
import com.klyx.i18n.strings
import com.klyx.lsp.LspActivityStore
import com.klyx.lsp.LspManager
import com.klyx.presentation.components.LspStatusBar
import com.klyx.presentation.viewmodel.EditorViewModel
import com.klyx.ui.provider.LocalTreeSitter
import io.github.rosemoe.sora.compose.CodeEditor
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.ExperimentalEditorApi
import io.github.rosemoe.sora.compose.content
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalEditorApi::class)
@Composable
fun TextFileEditor(
    tab: WorkspaceTab.TextFile,
    jbFontFamily: FontFamily,
    isDarkMode: Boolean,
    colorScheme: ColorScheme,
    selectionColors: TextSelectionColors,
    editorViewModel: EditorViewModel,
    registry: EditorStateRegistry
) {
    val settings = LocalAppSettings.current.editor
    val density = LocalDensity.current
    val treeSitter = LocalTreeSitter.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state = remember(tab.id) {
        registry[tab.id] ?: run {
            val baseline = registry.getBaselineText(tab.id) ?: ""
            CodeEditorState(context, scope).also {
                it.setText(baseline)
                it.registerInlayHintRenderers(
                    TextInlayHintRenderer.DefaultInstance,
                    ColorInlayHintRenderer.DefaultInstance
                )
                registry.register(tab.id, it)
            }
        }
    }

    val lspManager: LspManager = koinInject()
    val lspActivityStore: LspActivityStore = koinInject()
    val scheme = remember(isDarkMode, colorScheme, selectionColors) {
        KlyxEditorColorScheme(isDarkMode, colorScheme, selectionColors)
    }

    LaunchedEffect(settings) {
        state.applyEditorSettings(settings)
    }

    var isAccessoryBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(tab.id, state, treeSitter) {
        state.lineNumberMarginLeft = with(density) { 5.dp.toPx() }
        val baseLanguage = treeSitter.getLanguageForExtension(tab.file.extension)
        lspManager.onEditorCreated(tab.id, tab.file, tab.projectUri, state, baseLanguage)
    }

    LaunchedEffect(scheme, state.editorLanguage, state) {
        state.colorScheme = scheme
    }

    LaunchedEffect(tab.id, state) {
        state.content
            .drop(1)
            .debounce(500.milliseconds)
            .collect { content ->
                val baseline = registry.getBaselineText(tab.id) ?: ""
                editorViewModel.markTabModified(tab.id, content.toString() != baseline)
            }
    }

    LaunchedEffect(tab.id, state) {
        state.content
            .drop(1)
            .collect {
                editorViewModel.onContentChanged(tab.id)
            }
    }

    LaunchedEffect(state) {
        state.subscribeAlways<TextSizeChangeEvent> {
            val newSizeSp = with(density) { it.newTextSize.toSp().value }
            if (settings.fontSize != newSizeSp) {
                editorViewModel.updateFontSize(newSizeSp)
            }
        }
    }

    key(tab.id) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val fontManager: FontManager = koinInject()
                var currentFont by remember { mutableStateOf(JetBrainsMonoFontFamily) }
                LaunchedEffect(settings.customFontUri) {
                    currentFont = fontManager.getFontFamily(settings.customFontUri)
                }

                CodeEditor(
                    state = state,
                    fontFamily = currentFont,
                    fontSize = settings.fontSize.sp,
                    modifier = Modifier.fillMaxSize(),
                    wordWrap = settings.wordWrap
                )

                this@Column.AnimatedVisibility(
                    visible = !isAccessoryBarVisible,
                    modifier = Modifier
                        .align(BottomEnd)
                        .padding(16.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FilledIconButton(
                        onClick = { isAccessoryBarVisible = true },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = strings.showTools,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            LspStatusBar(lspActivityStore, lspManager)

            AnimatedVisibility(
                visible = isAccessoryBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) +
                        expandVertically(expandFrom = Alignment.Bottom) +
                        fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) +
                        shrinkVertically(shrinkTowards = Alignment.Bottom) +
                        fadeOut()
            ) {
                EditorAccessoryBar(
                    state = state,
                    fontFamily = jbFontFamily,
                    onHide = { isAccessoryBarVisible = false }
                )
            }
        }
    }
}
