package com.klyx

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.klyx.activities.KlyxActivity
import com.klyx.core.FpsTracker
import com.klyx.editor.compose.CodeEditor
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.ExperimentalComposeCodeEditorApi
import com.klyx.ui.theme.KlyxTheme
import com.klyx.ui.theme.rememberFontFamily

class TestActivity : KlyxActivity() {
    @OptIn(ExperimentalComposeCodeEditorApi::class)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(),
                            showLineNumber = true,
                            pinLineNumber = true,
                            fontSize = 18.sp,
                            fontFamily = rememberFontFamily("JetBrains Mono"),
                            state = remember { CodeEditorState("Yyoyooyooyoyoyoyo\nyoyoyoy\nyoyoyoyoyo\n".repeat(200)) }
                        )

                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            val fpsTracker = remember { FpsTracker() }
                            val fps by fpsTracker.fps

                            LaunchedEffect(Unit) {
                                fpsTracker.start()
                            }

                            Text("FPS $fps")
                        }
                    }
                }
            }
        }
    }
}
