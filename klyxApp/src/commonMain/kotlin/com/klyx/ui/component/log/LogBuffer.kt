package com.klyx.ui.component.log

import androidx.compose.runtime.Stable
import com.klyx.core.logging.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Stable
class LogBuffer(private val maxSize: Int = 2000) {
    private val _logs = MutableStateFlow<List<Message>>(emptyList())
    val logs = _logs.asStateFlow()

    fun add(log: Message) {
        _logs.update { (it + log).takeLast(maxSize) }
    }
}
