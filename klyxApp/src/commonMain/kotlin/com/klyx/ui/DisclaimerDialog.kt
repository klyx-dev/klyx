package com.klyx.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton
import com.klyx.resources.Res
import com.klyx.resources.disclaimer
import com.klyx.resources.exit
import com.klyx.resources.i_agree
import com.klyx.resources.important_notice
import com.klyx.ui.page.main.quitApp
import org.jetbrains.compose.resources.stringResource

@Composable
fun DisclaimerDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text(stringResource(Res.string.important_notice), textAlign = TextAlign.Center) },
        icon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SelectionContainer {
                    Text(stringResource(Res.string.disclaimer), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = { ConfirmButton(stringResource(Res.string.i_agree), onClick = onAccept) },
        dismissButton = { DismissButton(stringResource(Res.string.exit), ::quitApp) }
    )
}
