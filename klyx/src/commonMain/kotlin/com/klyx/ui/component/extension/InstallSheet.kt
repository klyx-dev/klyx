package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class InstallationType {
    Directory, Zip
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallSheet(
    onDismiss: () -> Unit,
    onPick: (InstallationType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
