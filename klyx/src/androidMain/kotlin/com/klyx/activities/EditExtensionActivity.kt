package com.klyx.activities

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.ui.page.extension.EditExtensionPage

class EditExtensionActivity : KlyxActivity() {
    @Composable
    override fun Content() {
        EditExtensionPage(
            edit = intent.getBooleanExtra("edit", false),
            filePath = intent.getStringExtra("filePath")!!,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        )
    }
}
