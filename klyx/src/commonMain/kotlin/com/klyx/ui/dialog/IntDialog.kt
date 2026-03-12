package com.klyx.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton

@Composable
fun IntDialog(
    value: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (value: Int) -> Unit = {},
    icon: ImageVector? = null,
    title: String? = null,
    description: String? = null,
    placeholder: String = "value",
    prefix: String? = null,
    suffix: String? = null,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
) {
    var value: Int? by rememberSaveable { mutableStateOf(value) }
    val isValid by remember {
        derivedStateOf { value in min..max }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton(enabled = isValid) {
                onConfirm(value!!)
                onDismissRequest()
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = title?.let { { Text(it, textAlign = TextAlign.Center) } },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                (description ?: title)?.let {
                    Text(
                        text = it,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                OutlinedTextField(
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    value = value?.toString() ?: "",
                    isError = !isValid,
                    supportingText = if (!isValid) {
                        {
                            SelectionContainer {
                                Text("min: $min, max: $max")
                            }
                        }
                    } else null,
                    onValueChange = {
                        value = (if (it.isEmpty()) null else it.toIntOrNull() ?: value)
                    },
                    placeholder = { Text(placeholder) },
                    suffix = suffix?.let { { Text(it) } },
                    prefix = prefix?.let { { Text(it) } },
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        }
    )
}
