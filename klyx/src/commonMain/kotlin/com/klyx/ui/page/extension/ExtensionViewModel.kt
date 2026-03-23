package com.klyx.ui.page.extension

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.io.fs
import com.klyx.extension.nodegraph.ExtensionManager
import com.klyx.nodegraph.GraphState
import com.klyx.nodegraph.restoreFromBytes
import com.klyx.nodegraph.toBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

class ExtensionViewModel : ViewModel() {
    var graphState by mutableStateOf<GraphState?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    fun loadOrCreateGraph(filePath: String, stateFactory: () -> GraphState) {
        if (graphState != null) return

        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val path = Path(filePath)
            val state = stateFactory()

            if (fs.exists(path)) {
                val bytes = fs.source(path).buffered().use { it.readByteArray() }
                withContext(Dispatchers.Main) {
                    state.restoreFromBytes(bytes)
                }
            }

            withContext(Dispatchers.Main) {
                graphState = state
                isLoading = false
            }
        }
    }

    fun saveGraph(filePath: String, extensionManager: ExtensionManager) {
        val state = graphState ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val bytes = state.toBytes()
            val path = Path(filePath)

            if (!fs.exists(path.parent!!)) {
                fs.createDirectories(path.parent!!)
            }

            fs.sink(path).buffered().use { it.write(bytes) }
            extensionManager.reloadExtension(path)
        }
    }
}
