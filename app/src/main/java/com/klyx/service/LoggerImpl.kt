package com.klyx.service

import android.util.Log
import com.klyx.api.data.log.LogEntry
import com.klyx.api.data.log.LogLevel
import com.klyx.api.service.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class LoggerImpl : Logger {

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    override val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    private val maxEntries = 1000

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        sourcePluginId: String?
    ) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            sourcePluginId = sourcePluginId
        )

        synchronized(_entries) {
            val current = _entries.value.toMutableList()
            if (current.size >= maxEntries) {
                current.removeAt(0)
            }
            current += entry
            _entries.update { current }
        }

        val logMessage =
            if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message
        when (level) {
            LogLevel.TRACE, LogLevel.DEBUG -> Log.d(tag, logMessage)
            LogLevel.INFO -> Log.i(tag, logMessage)
            LogLevel.WARN -> Log.w(tag, logMessage)
            LogLevel.ERROR -> Log.e(tag, logMessage)
        }
    }

    override fun clear() {
        _entries.update { emptyList() }
    }
}
