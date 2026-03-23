package com.klyx.activities

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.klyx.core.app.Debug
import com.klyx.editor.compose.ExperimentalComposeCodeEditorApi
import com.klyx.test.TestScreen

class TestActivity : KlyxActivity() {
    @OptIn(ExperimentalComposeCodeEditorApi::class)
    @Composable
    override fun Content() {
        TestScreen()
    }
}
