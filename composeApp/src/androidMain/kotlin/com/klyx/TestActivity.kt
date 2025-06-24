package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
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
                        Row {
                            Spacer(modifier = Modifier.width(2.dp))

                            CodeEditor(
                                state = rememberCodeEditorState("Hello"),
                                modifier = Modifier.fillMaxSize(),
                                editable = false,
                                pinLineNumber = false,
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
}
