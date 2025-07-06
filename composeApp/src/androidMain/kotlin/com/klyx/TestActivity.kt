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
import com.klyx.core.theme.ThemeParser
import com.klyx.core.theme.asColorScheme
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.ui.theme.KlyxTheme
import kotlinx.io.asSource
import kotlinx.io.buffered

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
                        LaunchedEffect(Unit) {
                            val themeSource = assets.open("themes/one/one.json").asSource()
                            ThemeParser.parse(themeSource.buffered().peek())
                                .onSuccess {
                                    println(it)
                                    it.asColorScheme()
                                }.onFailure {
                                    it.printStackTrace()
                                }
                        }
                    }
                }
            }
        }
    }
}

