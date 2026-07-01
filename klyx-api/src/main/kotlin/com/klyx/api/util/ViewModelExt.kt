package com.klyx.api.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Converts a [Flow] into a [StateFlow] that stays active while there are subscribers.
 * It remains active for [stopTimeoutMillis] after the last subscriber disconnects.
 */
context(vm: ViewModel)
fun <T> Flow<T>.stateInWhileSubscribed(initialValue: T, stopTimeoutMillis: Long = 5000): StateFlow<T> {
    return this.stateIn(
        scope = vm.viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = initialValue
    )
}

/**
 * Converts a [Flow] into a [StateFlow] that starts immediately.
 */
context(vm: ViewModel)
fun <T> Flow<T>.stateInEagerly(initialValue: T): StateFlow<T> {
    return this.stateIn(
        scope = vm.viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initialValue
    )
}

/**
 * Converts a [Flow] into a [StateFlow] that starts when the first subscriber connects.
 */
context(vm: ViewModel)
fun <T> Flow<T>.stateInLazily(initialValue: T): StateFlow<T> {
    return this.stateIn(
        scope = vm.viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = initialValue
    )
}

