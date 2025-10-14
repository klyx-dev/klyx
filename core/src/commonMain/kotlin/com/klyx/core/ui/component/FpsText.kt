package com.klyx.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.klyx.core.toFixed
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.nanoseconds

@Composable
fun FpsText(modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall) {
    var fps by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it.nanoseconds }

        while (isActive) {
            val now = withFrameNanos { it.nanoseconds }
            val delta = (now - lastFrameTime).inWholeNanoseconds
            lastFrameTime = now
            fps = 1000f / (delta / 1_000_000f)
        }
    }

    Text("FPS: ${fps.toFixed(1)}", modifier = modifier, style = style)
}
