package com.klyx.core

import androidx.compose.runtime.FloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos

@Stable
class FpsTracker {
    private var lastFrameTimeNs: Long = 0L
    private var _fps = mutableFloatStateOf(0f)

    val fps: FloatState get() = _fps

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
