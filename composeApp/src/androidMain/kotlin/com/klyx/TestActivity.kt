package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.klyx.activities.KlyxActivity
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.terminal.ubuntuProcess
import com.klyx.ui.theme.KlyxTheme
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.wasi.ExperimentalWasiApi

class TestActivity : KlyxActivity() {
    @OptIn(ExperimentalCodeEditorApi::class, ExperimentalWasmApi::class, ExperimentalWasiApi::class)
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
                        LaunchedEffect(Unit) {
                            ubuntuProcess("which", "pylsp"){
                                onOutput { println(it) }
                                onError { println(it) }
                            }.start()
                        }
                    }
                }
            }
        }
    }
}

