package com.klyx.extension.api.lsp

sealed class LanguageServerInstallationStatus {
    object None : LanguageServerInstallationStatus()
    object Downloading : LanguageServerInstallationStatus()
    object CheckingForUpdate : LanguageServerInstallationStatus()
    data class Failed(val reason: String) : LanguageServerInstallationStatus()

    override fun toString(): String = when (this) {
        None -> "None"
        Downloading -> "Downloading"
        CheckingForUpdate -> "CheckingForUpdate"
        is Failed -> "Failed($reason)"
    }
}

internal fun parseLanguageServerInstallationStatus(tag: Int, failedReason: String) = when (tag) {
    0 -> LanguageServerInstallationStatus.None
    1 -> LanguageServerInstallationStatus.Downloading
    2 -> LanguageServerInstallationStatus.CheckingForUpdate
    3 -> LanguageServerInstallationStatus.Failed(failedReason)
    else -> error("Unknown tag: $tag")
}
