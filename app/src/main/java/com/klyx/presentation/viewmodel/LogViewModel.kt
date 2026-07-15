package com.klyx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.api.data.log.LogEntry
import com.klyx.api.data.log.LogLevel
import com.klyx.api.service.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@KoinViewModel
class LogViewModel(
    private val logger: Logger
) : ViewModel() {

    private val _filterLevel = MutableStateFlow<LogLevel?>(null)
    val filterLevel: StateFlow<LogLevel?> = _filterLevel.asStateFlow()

    private val _filterText = MutableStateFlow("")
    val filterText: StateFlow<String> = _filterText.asStateFlow()

    val filteredEntries: StateFlow<List<LogEntry>> = combine(
        logger.entries,
        _filterLevel,
        _filterText
    ) { entries, level, text ->
        entries.filter { entry ->
            (level == null || entry.level == level) &&
                    (text.isBlank() || entry.tag.contains(text, ignoreCase = true) ||
                            entry.message.contains(text, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilterLevel(level: LogLevel?) {
        _filterLevel.value = level
    }

    fun setFilterText(text: String) {
        _filterText.value = text
    }

    fun clearLogs() {
        logger.clear()
    }

    fun getFormattedLogs(): String {
        val timeFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.getDefault())
        return filteredEntries.value.joinToString("\n") { entry ->
            val timestamp = timeFormat.format(Date(entry.timestamp))
            val level = entry.level.name.padEnd(5)
            val tag = entry.tag
            val message = entry.message
            val throwable = entry.throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
            "[$timestamp] $level/$tag: $message$throwable"
        }
    }
}
