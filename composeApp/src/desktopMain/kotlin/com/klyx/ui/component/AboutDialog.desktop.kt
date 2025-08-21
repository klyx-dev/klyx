package com.klyx.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.klyx.platform

@Composable
actual fun AboutDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Info") },
        text = {
            Text(
                """
                Klyx
                
                Platform: ${platform().name}
                OS (kernel) name: ${platform().os}
                OS architecture: ${platform().architecture}
            """.trimIndent()
            )
        },
        shape = RoundedCornerShape(12.dp),
        confirmButton = {}
    )
}
