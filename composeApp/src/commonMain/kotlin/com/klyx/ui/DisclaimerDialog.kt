package com.klyx.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.klyx.core.ui.component.KlyxDialog
import com.klyx.res.Res
import com.klyx.res.disclaimer
import com.klyx.res.exit
import com.klyx.res.i_agree
import com.klyx.res.important_notice
import com.klyx.ui.page.main.quitApp
import org.jetbrains.compose.resources.stringResource

@Composable
fun DisclaimerDialog(
    onAccept: () -> Unit,
) {
    KlyxDialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(stringResource(Res.string.important_notice))
        },
        icon = {
            Icon(
                Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SelectionContainer {
                    Text(stringResource(Res.string.disclaimer))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(stringResource(Res.string.i_agree))
            }
        },
        dismissButton = {
            TextButton(onClick = ::quitApp) {
                Text(stringResource(Res.string.exit))
            }
        }
    )
}
