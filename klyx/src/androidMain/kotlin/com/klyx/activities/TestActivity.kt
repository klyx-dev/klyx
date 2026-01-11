package com.klyx.activities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.klyx.core.ui.component.FpsText
import com.klyx.editor.compose.ExperimentalComposeCodeEditorApi

class TestActivity : KlyxActivity() {
    @OptIn(ExperimentalComposeCodeEditorApi::class)
    @Composable
    override fun Content() {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    FpsText()
                }
            }
        }
    }
}
