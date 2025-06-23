package com.klyx.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos

@Stable
class FpsTracker {
    private var lastFrameTimeNs: Long = 0L
    private var _fps = mutableStateOf(0f)

    val fps get() = _fps.asFloatState()

    suspend fun start() {
        lastFrameTimeNs = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val deltaMs = (now - lastFrameTimeNs) / 1_000_000f
            lastFrameTimeNs = now

            val fpsValue = 1000f / deltaMs
            _fps.value = fpsValue
        }
    }
}
