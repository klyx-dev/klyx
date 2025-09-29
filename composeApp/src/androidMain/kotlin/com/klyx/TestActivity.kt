package com.klyx

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.klyx.activities.KlyxActivity
import com.klyx.editor.compose.CodeEditor
import com.klyx.ui.theme.KlyxTheme

class TestActivity : KlyxActivity() {
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
                                .imePadding()
                        )
                    }
                }
            }
        }
    }
}
