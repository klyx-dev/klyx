package com.klyx.editor.lsp.util

import com.klyx.core.process.Command
import com.klyx.core.settings.CommandSettings

fun CommandSettings.toCommand() = path?.let {
    Command.newCommand(it)
        .args(arguments.orEmpty())
        .env(env.orEmpty())
}
