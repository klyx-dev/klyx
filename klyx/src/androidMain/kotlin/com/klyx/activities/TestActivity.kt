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
        Scaffold { innerPadding ->
            val textMeasurer = rememberTextMeasurer()

            Debug {
                val textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                val result1 = textMeasurer.measure("Mg", style = textStyle)
                val result2 = textMeasurer.measure("X", style = textStyle)
                println("result1: ${result1.size}")
                println("result2: ${result2.size}")
                println("${result2.size.height - result2.firstBaseline}")
            }

            TestScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
