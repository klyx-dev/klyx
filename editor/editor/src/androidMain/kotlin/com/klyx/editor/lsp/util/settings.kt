package com.klyx.editor.lsp.util

import com.klyx.core.settings.CommandSettings
import com.klyx.extension.internal.Command

fun CommandSettings.toCommand() = path?.let {
    Command(
        command = it,
        args = arguments.orEmpty(),
        env = env.orEmpty()
    )
}
