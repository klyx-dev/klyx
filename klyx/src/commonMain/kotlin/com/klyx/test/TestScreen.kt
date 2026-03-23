package com.klyx.test

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.klyx.core.io.fs
import com.klyx.core.io.homeDir
import com.klyx.core.ui.component.FpsText
import com.klyx.core.util.join
import com.klyx.extension.nodegraph.EditorLspExtension
import com.klyx.extension.nodegraph.MetadataExtension
import com.klyx.nodegraph.GraphEditor
import com.klyx.nodegraph.rememberGraphState
import com.klyx.nodegraph.restoreFromBytes
import com.klyx.nodegraph.toBytes
import kotlinx.io.buffered
import kotlinx.io.readByteArray

@Composable
fun TestScreen() {
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        val graphState = rememberGraphState(includeDefaultStartNode = true) {
            install(MetadataExtension, EditorLspExtension)
        }

        GraphEditor(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            state = graphState
        )

        Column(Modifier.align(Alignment.BottomEnd).navigationBarsPadding()) {
            Button(onClick = {
                fs.sink(homeDir().join("Klyx/graph.kxng")).buffered().use {
                    it.write(graphState.toBytes())
                }
            }) {
                Text("Save")
            }

            Button(onClick = {
                fs.source(homeDir().join("Klyx/graph.kxng")).buffered().use {
                    graphState.restoreFromBytes(it.readByteArray())
                }
            }) {
                Text("Load")
            }
        }

        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd,
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                FpsText()
            }
        }
    }
}
