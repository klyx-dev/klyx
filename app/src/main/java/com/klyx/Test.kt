package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klyx.core.rememberTypeface
import com.klyx.editor.compose.KlyxCodeEditor

class Test : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val code = """
            fun main() {
                println("Hellow")
            }
        """.trimIndent()
        setContent {
            var wrap by remember { mutableStateOf(false) }
            val typeface by rememberTypeface("IBM Plex Mono")

            Box(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        wrap = !wrap
                    }
            ) {
                KlyxCodeEditor(
                    code,
                    wrapText = wrap,
                    scrollbarThickness = 12.dp,
                    typeface = typeface
                )
            }
        }
    }
}
