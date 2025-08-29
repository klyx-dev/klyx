package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klyx.core.extension.ExtensionFilter
import com.klyx.core.extension.ExtensionToml

enum class InstallationType {
    Directory, Zip
}

@Composable
expect fun ExtensionScreen(modifier: Modifier = Modifier)

@Composable
private fun ExtensionFilterBar(
    selected: ExtensionFilter,
    onFilterChange: (ExtensionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExtensionFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.name) }
            )
        }
    }
}

@Composable
fun ExtensionCard(extension: ExtensionToml) {

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallSheet(
    onDismiss: () -> Unit,
    onPick: (InstallationType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Install Extension", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = { onPick(InstallationType.Directory) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("From Directory")
            }
            TextButton(
                onClick = { onPick(InstallationType.Zip) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("From Zip")
            }
        }
    }
}
