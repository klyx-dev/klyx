package com.klyx.lsp

import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkDoneProgressNotification {
    val kind: WorkDoneProgressKind
}
