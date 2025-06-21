package com.klyx.ui.component

import androidx.compose.runtime.Composable
import com.klyx.BuildConfig
import com.klyx.platform

@Composable
actual fun AboutDialog(onDismissRequest: () -> Unit) {
    KlyxDialog(
        onDismissRequest = onDismissRequest,
        title = "Info",
        message = """
            Klyx ${BuildConfig.VERSION_NAME}
            
            Platform: ${platform().name}
            OS: ${platform().os}
        """.trimIndent()
    )
}
