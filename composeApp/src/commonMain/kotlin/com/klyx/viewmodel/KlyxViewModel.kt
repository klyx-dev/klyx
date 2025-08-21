package com.klyx.viewmodel

import androidx.lifecycle.ViewModel
import com.klyx.core.Notifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KlyxMenuState(
    val showAboutDialog: Boolean = false
)

class KlyxViewModel(
    private val notifier: Notifier
) : ViewModel() {
    private val _klyxMenuState = MutableStateFlow(KlyxMenuState())
    val klyxMenuState = _klyxMenuState.asStateFlow()

    fun showAboutDialog() {
        _klyxMenuState.update { it.copy(showAboutDialog = true) }
    }

    fun dismissAboutDialog() {
        _klyxMenuState.update { it.copy(showAboutDialog = false) }
    }
}
