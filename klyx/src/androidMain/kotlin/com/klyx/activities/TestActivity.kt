package com.klyx.activities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.klyx.core.ui.component.FpsText
import com.klyx.editor.compose.CodeEditor
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.ExperimentalComposeCodeEditorApi
import com.klyx.ui.theme.rememberFontFamily

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
                CodeEditor(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    showLineNumber = true,
                    pinLineNumber = true,
                    fontSize = 18.sp,
                    fontFamily = rememberFontFamily("JetBrains Mono"),
                    state = remember { CodeEditorState("Yyoyooyooyoyoyoyo\nyoyoyoy\nyoyoyoyoyo\n".repeat(200)) }
                )

                BasicText("")
                BasicTextField("", onValueChange = {})
                SelectionContainer { }

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
