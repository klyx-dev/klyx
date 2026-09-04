package com.klyx.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.data.editor.EditorStateRegistry
import com.klyx.i18n.strings
import com.klyx.icons.Klyx
import com.klyx.icons.KlyxIcons
import com.klyx.presentation.components.WelcomeScreen
import com.klyx.presentation.viewmodel.EditorViewModel
import com.klyx.ui.theme.uiFontFamily
import kotlinx.collections.immutable.ImmutableList

@Composable
fun MainContent(
    paddingValues: PaddingValues,
    editorViewModel: EditorViewModel,
    openTabs: ImmutableList<WorkspaceTab>,
    activeTab: WorkspaceTab?,
    jbFontFamily: FontFamily,
    registry: EditorStateRegistry,
    onOpenProjectClick: () -> Unit,
    onNewFileClick: () -> Unit
) {
    if (openTabs.isEmpty()) {
        WelcomeScreen(
            onNewFileClick = onNewFileClick,
            onOpenProjectClick = onOpenProjectClick,
            modifier = Modifier.padding(paddingValues)
        )
    } else {
        val pagerState = rememberPagerState(pageCount = { openTabs.size })

        LaunchedEffect(activeTab) {
            val index = openTabs.indexOfFirst { it == activeTab }
            if (index != -1 && index != pagerState.currentPage) {
                pagerState.scrollToPage(index)
            }
        }

        val isDarkMode = LocalIsDarkMode.current
        val colorScheme = MaterialTheme.colorScheme

        EditorPager(
            pagerState = pagerState,
            openTabs = openTabs,
            paddingValues = paddingValues,
            jbFontFamily = jbFontFamily,
            isDarkMode = isDarkMode,
            colorScheme = colorScheme,
            editorViewModel = editorViewModel,
            registry = registry,
            onNewFileClick = onNewFileClick,
            onOpenProjectClick = onOpenProjectClick
        )
    }
}

@Composable
fun EditorEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = KlyxIcons.Klyx,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "K L Y X",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = uiFontFamily(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.swipeHintHome,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = uiFontFamily(),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}
