package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.klyx.appPrefs
import com.klyx.res.Res
import com.klyx.res.extension_disclaimer
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExtensionScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Navigator(ExtensionListScreen())

        var showExtensionDisclaimer by remember {
            mutableStateOf(
                appPrefs.getBoolean(
                    "show_extension_disclaimer",
                    true
                )
            )
        }

        if (showExtensionDisclaimer) {
            AlertDialog(
                onDismissRequest = { showExtensionDisclaimer = false },
                confirmButton = {
                    TextButton(onClick = { showExtensionDisclaimer = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Info") },
                text = { Text(stringResource(Res.string.extension_disclaimer)) },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExtensionDisclaimer = false
                            appPrefs.putBoolean("show_extension_disclaimer", false)
                        }
                    ) {
                        Text("Don't show again")
                    }
                }
            )
        }
    }
}

