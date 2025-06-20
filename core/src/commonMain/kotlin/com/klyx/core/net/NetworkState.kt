package com.klyx.core.net

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

sealed class NetworkState {
    data object NotConnected : NetworkState()
    data object Connected : NetworkState()
}

val NetworkState.isConnected: Boolean
    get() = this is NetworkState.Connected

val NetworkState.isNotConnected: Boolean
    get() = this is NetworkState.NotConnected

@Composable
expect fun rememberNetworkState(): State<NetworkState>
