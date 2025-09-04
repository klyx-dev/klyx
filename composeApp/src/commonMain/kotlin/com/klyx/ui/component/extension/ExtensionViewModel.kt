package com.klyx.ui.component.extension

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.extension.ExtensionInfo
import com.klyx.core.extension.fetchExtensionsFlow
import com.klyx.core.logging.logger
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class ExtensionViewModel : ViewModel() {
    val extensions = mutableStateListOf<ExtensionInfo>()
    var isLoading by mutableStateOf(false)
        private set

    private var hasLoaded = false

    fun loadExtensions() {
        if (hasLoaded) return
        hasLoaded = true

        viewModelScope.launch {
            try {
                fetchExtensionsFlow()
                    .onStart { isLoading = true }
                    .onCompletion { isLoading = false }
                    .collect { info ->
                        if (extensions.none { it.id == info.id }) {
                            extensions += info
                        }
                    }
            } catch (err: Exception) {
                logger().error { "Error fetching extensions: $err" }
            }
        }
    }
}
