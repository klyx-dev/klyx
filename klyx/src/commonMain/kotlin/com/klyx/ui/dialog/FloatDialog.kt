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
fun FloatDialog(
    value: Float,
    onDismissRequest: () -> Unit,
    onConfirm: (value: Float) -> Unit = {},
    icon: ImageVector? = null,
    title: String? = null,
    description: String? = null,
    placeholder: String = "value",
    prefix: String? = null,
    suffix: String? = null,
    min: Float = Float.MIN_VALUE,
    max: Float = Float.MAX_VALUE,
    minInclusive: Boolean = true,
    maxInclusive: Boolean = true,
) {
    var value by rememberSaveable { mutableStateOf(value.toString()) }
    val isValid by remember(minInclusive, maxInclusive, min, max) {
        derivedStateOf {
            val value = value.toFloatOrNull()
            when {
                value != null -> {
                    when {
                        minInclusive && !maxInclusive -> value in min..<max
                        !minInclusive && maxInclusive -> value > min && value <= max
                        minInclusive && maxInclusive -> value in min..max
                        else -> value > min && value < max
                    }
                }

                else -> false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton(enabled = isValid) {
                onConfirm(value.toFloat())
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
                    value = value,
                    isError = !isValid,
                    supportingText = if (!isValid) {
                        {
                            SelectionContainer {
                                Text(
                                    when {
                                        minInclusive && !maxInclusive -> "min: $min, max: less than $max"
                                        !minInclusive && maxInclusive -> "min: greater than $min, max: $max"
                                        minInclusive && maxInclusive -> "min: $min, max: $max"
                                        else -> "min: greater than $min, max: less than $max"
                                    }
                                )
                            }
                        }
                    } else null,
                    onValueChange = { value = it },
                    placeholder = { Text(placeholder) },
                    suffix = suffix?.let { { Text(it) } },
                    prefix = prefix?.let { { Text(it) } },
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
