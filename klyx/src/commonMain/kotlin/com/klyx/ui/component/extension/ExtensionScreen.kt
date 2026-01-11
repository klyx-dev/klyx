package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.klyx.appPrefs
import com.klyx.core.io.okioFs
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton
import com.klyx.resources.Res.string
import com.klyx.resources.extension_disclaimer
import com.klyx.ui.component.extension.ExtensionRoutes.ExtensionDetail
import com.klyx.ui.component.extension.ExtensionRoutes.ExtensionList
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExtensionScreen(modifier: Modifier = Modifier) {

    val backStack = rememberNavBackStack(ExtensionRoutes.config(), ExtensionList)

    NavDisplay(
        modifier = modifier.padding(horizontal = 8.dp),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<ExtensionList> {
                ExtensionListScreen(
                    modifier = Modifier.fillMaxSize(),
                    onExtensionItemClick = { manifest ->
                        backStack.add(ExtensionDetail(manifest))
                    }
                )
            }

            entry<ExtensionDetail> { detail ->
                ExtensionDetailScreen(
                    modifier = Modifier.fillMaxSize(),
                    manifest = detail.manifest,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )

    ExtensionDisclaimer()
}

@Composable
private fun ExtensionDisclaimer() {
    var showExtensionDisclaimer by remember {
        mutableStateOf(appPrefs.getBoolean("show_extension_disclaimer", true))
    }

    if (showExtensionDisclaimer) {
        AlertDialog(
            onDismissRequest = { showExtensionDisclaimer = false },
            confirmButton = { ConfirmButton("OK") { showExtensionDisclaimer = false } },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("Info", textAlign = TextAlign.Center) },
            text = { Text(stringResource(string.extension_disclaimer), style = MaterialTheme.typography.bodyLarge) },
            dismissButton = {
                DismissButton("Don't show again") {
                    showExtensionDisclaimer = false
                    appPrefs.putBoolean("show_extension_disclaimer", false)
                }
            }
        )
    }
}

