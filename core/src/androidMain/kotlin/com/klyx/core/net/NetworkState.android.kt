package com.klyx.core.net

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.klyx.core.isInternetAvailable

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
@Composable
actual fun rememberNetworkState(): State<NetworkState> {
    val context = LocalContext.current
    val isConnected = context.isInternetAvailable()
    return rememberUpdatedState(if (isConnected) NetworkState.Connected else NetworkState.NotConnected)
}
