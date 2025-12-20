package com.klyx.ui.page.settings.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.core.settings.EditorSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton
import com.klyx.resources.Res.string
import com.klyx.resources.font_size
import com.klyx.resources.font_size_desc
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue

@Composable
fun FontSizeDialog(
    settings: EditorSettings,
    onDismissRequest: () -> Unit
) {
    var fontSize by rememberSaveable { mutableStateOf(settings.fontSize.toString()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton {
                settings.update { it.copy(fontSize = fontSize.toFloatOrNull()?.absoluteValue ?: 14f) }
                onDismissRequest()
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        icon = { Icon(Icons.Outlined.TextFields, contentDescription = null) },
        title = { Text(stringResource(string.font_size), textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(string.font_size_desc),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                OutlinedTextField(
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    placeholder = { Text("14") },
                    suffix = { Text("sp") },
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }
        }
    )
}
