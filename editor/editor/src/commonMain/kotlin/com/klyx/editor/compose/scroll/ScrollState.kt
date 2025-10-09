package com.klyx.editor.compose.scroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
class ScrollState(
    initialX: Float = 0f,
    initialY: Float = 0f
) {
    private val animX = Animatable(initialX)
    private val animY = Animatable(initialY)

    var offsetX by mutableStateOf(initialX)
        private set
    var offsetY by mutableStateOf(initialY)
        private set

    fun scroll(dx: Float, dy: Float) {
        offsetX = (offsetX + dx).coerceAtLeast(0f)
        offsetY = (offsetY + dy).coerceAtLeast(0f)
    }

    fun scrollTo(x: Float, y: Float) {
        offsetX = x.coerceAtLeast(0f)
        offsetY = y.coerceAtLeast(0f)
    }

    suspend fun smoothScrollBy(dx: Float, dy: Float, duration: Int = 300): Unit = coroutineScope {
        launch {
            animX.snapTo(offsetX)
            animX.animateTo(
                targetValue = maxOf(0f, offsetX + dx),
                animationSpec = tween(durationMillis = duration)
            ) {
                offsetX = value
            }
        }

        launch {
            animY.snapTo(offsetY)
            animY.animateTo(
                targetValue = maxOf(0f, offsetY + dy),
                animationSpec = tween(durationMillis = duration)
            ) {
                offsetY = value
            }
        }
    }

    suspend fun smoothScrollTo(x: Float, y: Float, duration: Int = 300): Unit = coroutineScope {
        launch {
            animX.snapTo(offsetX)
            animX.animateTo(maxOf(0f, x), animationSpec = tween(durationMillis = duration)) {
                offsetX = value
            }
        }
        launch {
            animY.snapTo(offsetY)
            animY.animateTo(maxOf(0f, y), animationSpec = tween(durationMillis = duration)) {
                offsetY = value
            }
        }
    }
}
