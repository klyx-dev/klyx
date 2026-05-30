package com.klyx.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

context(vm: ViewModel)
fun <T> Flow<T>.stateInWhileSubscribed(initialValue: T, stopTimeoutMillis: Long = 5000): StateFlow<T> {
    return this.stateIn(
        scope = vm.viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = initialValue
    )
}

context(vm: ViewModel)
fun <T> Flow<T>.stateInEagerly(initialValue: T): StateFlow<T> {
    return this.stateIn(
        scope = vm.viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initialValue
    )
}

context(vm: ViewModel)
fun <T> Flow<T>.stateInLazily(initialValue: T): StateFlow<T> {
    return this.stateIn(
        scope = vm.viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = initialValue
    )
}

