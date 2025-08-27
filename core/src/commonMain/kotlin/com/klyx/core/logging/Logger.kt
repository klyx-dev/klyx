package com.klyx.core.logging

import kotlinx.coroutines.flow.MutableSharedFlow

internal val loggers = mutableMapOf<String, KxLogger>()

interface Logger {
    fun log(message: Message)
}

internal class FlowLogger(
    private val flow: MutableSharedFlow<Message>
) : Logger {
    override fun log(message: Message) {
        flow.tryEmit(message)
    }
}
