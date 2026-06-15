package com.klyx.presentation.screen.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.klyx.data.fs.Paths
import com.klyx.presentation.components.dialogs.TerminalWipeConfirmationDialog
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.SettingScreens
import com.klyx.presentation.screen.settings.components.SettingsItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.terminal.prefix
import com.klyx.terminal.versionFile
import com.klyx.ui.widgets.LocalToastHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreens.DeveloperOptions() {
    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val toastHostState = LocalToastHostState.current

    var showWipeDialog by remember { mutableStateOf(false) }

    if (showWipeDialog) {
        TerminalWipeConfirmationDialog(
            onDismiss = { showWipeDialog = false },
            onConfirm = {
                showWipeDialog = false
                scope.launch(Dispatchers.IO) {
                    if (Paths.prefix.exists()) Paths.prefix.deleteRecursively()
                    if (Paths.versionFile.exists()) Paths.versionFile.delete()
                    toastHostState.showToast("Terminal environment wiped")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Developer Options") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp
            )
        ) {
            item {
                SettingsSubsection("Terminal Testing") {
                    SettingsItem(
                        title = "Wipe Terminal Environment",
                        subtitle = "Deletes the prefix and version file to force a reinstall",
                        onClick = { showWipeDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}
