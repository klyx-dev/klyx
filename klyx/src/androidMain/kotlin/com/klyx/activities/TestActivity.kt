package com.klyx.activities

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.editor.compose.ExperimentalComposeCodeEditorApi
import com.klyx.test.TestScreen

class TestActivity : KlyxActivity() {
    @OptIn(ExperimentalComposeCodeEditorApi::class)
    @Composable
    override fun Content() {
        Scaffold { innerPadding ->
            TestScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
