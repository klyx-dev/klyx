package com.klyx.lsp

import com.klyx.runtime.OperatingSystem
import com.klyx.runtime.currentOperatingSystem
import com.klyx.runtime.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
class TermuxPermissionConsent(private val settings: SettingsStore) {

    val showPrompt: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private var dismissedThisSession = false

    suspend fun canStart(config: LspServerConfig): Boolean {
        if (currentOperatingSystem != OperatingSystem.Android) return true
        val usesTermux = config.transportType == LspTransportType.Termux ||
                (config.transportType == LspTransportType.Auto && TermuxBridge.isTermuxInstalled())
        if (!usesTermux || TermuxBridge.hasRunCommandPermission()) return true
        if (!settings.read().hideTermuxPermissionPrompt && !dismissedThisSession) {
            showPrompt.value = true
        }
        return false
    }

    suspend fun dismiss(dontAskAgain: Boolean) {
        if (dontAskAgain) settings.update { it.copy(hideTermuxPermissionPrompt = true) }
        dismissedThisSession = true
        showPrompt.value = false
    }
}
