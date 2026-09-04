package com.klyx.presentation.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import com.klyx.CrashHandler
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.data.editor.EditorStateRegistry
import com.klyx.presentation.components.FileSystemImage
import com.klyx.presentation.components.WelcomeScreen
import com.klyx.presentation.viewmodel.EditorViewModel
import kotlinx.collections.immutable.ImmutableList
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun EditorPager(
    pagerState: PagerState,
    openTabs: ImmutableList<WorkspaceTab>,
    paddingValues: PaddingValues,
    jbFontFamily: FontFamily,
    isDarkMode: Boolean,
    colorScheme: ColorScheme,
    editorViewModel: EditorViewModel,
    registry: EditorStateRegistry,
    onNewFileClick: () -> Unit,
    onOpenProjectClick: () -> Unit
) {
    val selectionColors = LocalTextSelectionColors.current

    CrashHandler.currentTabId = null
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        beyondViewportPageCount = 1,
        key = { openTabs[it].id },
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .imePadding()
    ) { pageIndex ->
        when (val tab = openTabs[pageIndex]) {
            is WorkspaceTab.TextFile -> {
                TextFileEditor(
                    tab = tab,
                    jbFontFamily = jbFontFamily,
                    isDarkMode = isDarkMode,
                    colorScheme = colorScheme,
                    selectionColors = selectionColors,
                    editorViewModel = editorViewModel,
                    registry = registry
                )
            }

            is WorkspaceTab.ImageFile -> {
                val zoomState = rememberZoomState(maxScale = 100f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    FileSystemImage(
                        uri = tab.uri,
                        contentDescription = tab.title,
                        contentScale = Fit,
                        filterQuality = High,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomState = zoomState)
                    )
                }
            }

            is WorkspaceTab.Welcome -> {
                WelcomeScreen(
                    onNewFileClick = onNewFileClick,
                    onOpenProjectClick = onOpenProjectClick
                )
            }

            is WorkspaceTab.Custom -> {
                CrashHandler.currentTabId = tab.id
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    tab.content()
                }
            }
        }
    }
}
