package com.klyx.core.logging

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object KxLog {
    private var globalConfig = LoggerConfig.Default

    private val _globalLogFlow = MutableSharedFlow<Message>(
        replay = globalConfig.replayBufferSize,
        extraBufferCapacity = globalConfig.bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val logFlow: Flow<Message> = _globalLogFlow.asSharedFlow()

    internal val globalFlowLogger = FlowLogger(_globalLogFlow)

    fun configure(config: LoggerConfig) {
        globalConfig = config
        LoggerConfig.Default = config
    }

    inline fun <reified T> logger(): KxLogger = logger(T::class.simpleName ?: "Unknown")

    fun logger(tag: String): KxLogger = KxLogger.getLogger(tag, globalConfig)

    fun v(tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        logger(tag).verbose(message, metadata)
    }

    fun d(tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        logger(tag).debug(message, metadata)
    }

    fun i(tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        logger(tag).info(message, metadata)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        logger(tag).warn(message, throwable, metadata)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        logger(tag).error(message, throwable, metadata)
    }
}
