package com.klyx.ui.util

import androidx.compose.material3.DrawerState

suspend fun DrawerState.openIfClosed() {
    if (isClosed) open()
}
