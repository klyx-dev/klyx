package com.klyx.ui.page.settings.terminal

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.klyx.NavigationScope
import com.klyx.core.settings.LocalAppSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceInfo
import com.klyx.core.ui.component.PreferenceItem
import com.klyx.core.ui.component.PreferenceSubtitle
import com.klyx.core.ui.component.PreferenceSwitch
import com.klyx.icons.Icons
import com.klyx.icons.Terminal2
import com.klyx.icons.TextCursor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
context(scope: NavigationScope)
fun TerminalSettingsPage(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current.terminal
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Terminal Settings") },
                navigationIcon = { BackButton(scope.navigator::navigateBack) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->

        var showCursorStyleDialog by rememberSaveable { mutableStateOf(false) }

        LazyColumn(contentPadding = paddingValues) {
            item {
                PreferenceInfo("Changes to these settings require restarting the terminal screen to take effect.")
            }

            item { PreferenceSubtitle("Terminal") }

            item {
                PreferenceItem(
                    title = "Cursor Style",
                    icon = Icons.TextCursor,
                    description = settings.cursorStyle.name,
                    onClick = { showCursorStyleDialog = true }
                )
            }

            item {
                PreferenceSwitch(
                    title = "Open Terminal as \"root\"",
                    description = "Start the terminal with root privileges instead of a normal user session.",
                    icon = Icons.Terminal2,
                    checked = settings.openAsRoot,
                    onCheckedChange = { checked ->
                        settings.update { it.copy(openAsRoot = checked) }
                    }
                )
            }
        }

        if (showCursorStyleDialog) {
            CursorStyleDialog(
                settings = settings,
                onDismissRequest = { showCursorStyleDialog = false }
            )
        }
    }
}
