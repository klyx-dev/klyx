@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.ui.page.main

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klyx.LocalDrawerState
import com.klyx.core.settings.currentAppSettings
import com.klyx.core.ui.component.FpsText
import com.klyx.extension.api.Project
import com.klyx.tab.Tab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    isTabOpen: Boolean,
    activeTab: Tab?,
    project: Project,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val commonActions = remember(project) { commonTopBarActions(project) }

    if (isTabOpen) {
        TopAppBar(
            title = {
                activeTab?.let {
                    Text(
                        text = it.name,
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis
                    )
                }
            },
            subtitle = { if (currentAppSettings.showFps) FpsText() },
            navigationIcon = { FileTreeButton() },
            actions = commonActions
        )
    } else {
        LargeFlexibleTopAppBar(
            title = { Text("Klyx") },
            subtitle = if (currentAppSettings.showFps) {
                { FpsText() }
            } else null,
            scrollBehavior = scrollBehavior,
            navigationIcon = { FileTreeButton() },
            actions = commonActions
        )
    }
}

@Composable
private fun FileTreeButton() {
    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    FilledIconButton(
        onClick = { scope.launch { drawerState.open() } },
        shapes = IconButtonDefaults.shapes(
            shape = IconButtonDefaults.mediumSquareShape,
            pressedShape = IconButtonDefaults.mediumPressedShape
        ),
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.DriveFileMove,
            contentDescription = "Open file tree"
        )
    }
}
