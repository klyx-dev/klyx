package com.klyx

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.ui.ComposeActivity
import io.github.rosemoe.sora.compose.ExperimentalEditorApi

class TestActivity : ComposeActivity() {

    @OptIn(ExperimentalEditorApi::class)
    @Composable
    override fun BoxScope.Content() {
        Surface(Modifier.fillMaxSize()) {

        }
    }
}
