package com.klyx.ui.component.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.klyx.core.Notifier
import com.klyx.core.extension.ExtensionInfo
import com.klyx.core.extension.fetchAllExtensions
import com.klyx.core.logging.logger
import com.klyx.core.string
import com.klyx.core.value
import com.klyx.extension.ExtensionManager
import com.klyx.res.Res.string
import com.klyx.res.extension_install_failed
import com.klyx.res.extension_install_success
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExtensionListState(
    val extensionInfos: List<ExtensionInfo> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface ExtensionEvent {
    data object ReloadExtensions : ExtensionEvent
}

class ExtensionViewModel(private val notifier: Notifier) : ViewModel() {
    private val _extensionListState = MutableStateFlow(ExtensionListState())
    val extensionListState = _extensionListState.asStateFlow()

    private val extensionEventChannel = Channel<ExtensionEvent>()
    val extensionEvents = extensionEventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            extensionEventChannel.send(ExtensionEvent.ReloadExtensions)
        }
    }

    fun loadExtensions() {
        _extensionListState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                _extensionListState.update {
                    it.copy(
                        extensionInfos = fetchAllExtensions()
                    )
                }
            } catch (err: Throwable) {
                logger().error(err) { "Error fetching extensions: ${err.message}" }
            }

            _extensionListState.update { it.copy(isLoading = false) }
        }
    }

    suspend fun installExtension(info: ExtensionInfo) {
        com.klyx.core.extension.installExtension(info).onSuccess { file ->
            ExtensionManager.installExtension(
                directory = file,
                isDevExtension = false
            ).fold(
                failure = {
                    notifier.error(string(string.extension_install_failed, it))
                },
                success = {
                    notifier.success(string.extension_install_success.value)
                }
            )
        }.onFailure {
            notifier.error(
                string(
                    string.extension_install_failed,
                    it.message ?: it.stackTraceToString()
                )
            )
        }
    }
}
