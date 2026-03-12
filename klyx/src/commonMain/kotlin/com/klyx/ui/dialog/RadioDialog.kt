package com.klyx.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.core.ui.component.DismissButton

@Composable
fun <T> RadioDialog(
    values: List<T>,
    onValueChange: (newValue: T) -> Unit,
    onDismissRequest: () -> Unit,
    label: @Composable (T) -> Unit,
    selectedValue: T? = null,
    icon: ImageVector? = null,
    title: String? = null,
    description: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { DismissButton { onDismissRequest() } },
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = title?.let { { Text(it, textAlign = TextAlign.Center) } },
        text = {
            LazyColumn {
                if (description != null) {
                    stickyHeader {
                        Column {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                items(values) { value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onValueChange(value) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selectedValue,
                            onClick = { onValueChange(value) }
                        )

                        label(value)
                    }
                }
            }
        }
    )
}
