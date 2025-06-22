package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.klyx.editor.CodeEditor
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.rememberCodeEditorState
import com.klyx.ui.theme.KlyxTheme
import com.klyx.ui.theme.provider

class TestActivity : ComponentActivity() {
    @OptIn(ExperimentalCodeEditorApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KlyxTheme {
                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        CodeEditor(
                            state = rememberCodeEditorState("Hello"),
                            modifier = Modifier.fillMaxSize(),
                            fontFamily = FontFamily(
                                Font(
                                    googleFont = GoogleFont("JetBrains Mono"),
                                    fontProvider = provider,
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
