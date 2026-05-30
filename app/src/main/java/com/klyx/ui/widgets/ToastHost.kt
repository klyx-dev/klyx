package com.klyx.ui.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.klyx.presentation.components.FullscreenPopup
import com.klyx.ui.animation.LocalReduceMotion
import com.klyx.ui.animation.lessSpringySpec
import com.klyx.ui.animation.orSnap
import com.klyx.ui.provider.LocalScreenSize
import com.klyx.ui.theme.blend
import com.klyx.ui.theme.harmonizeWithPrimary
import com.klyx.util.extractMessage
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ToastHost(
    modifier: Modifier = Modifier,
    state: ToastHostState = LocalToastHostState.current,
    alignment: Alignment = Alignment.BottomCenter,
    transitionSpec: AnimatedContentTransitionScope<ToastData?>.(reduceMotion: Boolean) -> ContentTransform = {
        ToastDefaults.transition(
            it
        )
    },
    toast: @Composable (ToastData) -> Unit = { Toast(it) },
    enableSwipes: Boolean = true
) {
    val screenSize = LocalScreenSize.current
    val sizeMin = screenSize.width.coerceAtMost(screenSize.height)

    val currentToastData = state.currentToastData

    val accessibilityManager = LocalAccessibilityManager.current

    LaunchedEffect(currentToastData) {
        if (currentToastData != null) {
            val duration = currentToastData.visuals.duration.toMillis(accessibilityManager)
            delay(duration.milliseconds)
            currentToastData.dismiss()
        }
    }

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val threshold = 300f

    val reduceMotion = LocalReduceMotion.current

    FullscreenPopup(placeAboveAll = true) {
        AnimatedContent(
            modifier = Modifier.zIndex(100f),
            targetState = currentToastData,
            transitionSpec = { transitionSpec(reduceMotion) }
        ) { data ->
            if (enableSwipes) {
                val reset: CoroutineScope.() -> Unit = {
                    launch {
                        alpha.animateTo(
                            targetValue = 1f,
                            animationSpec = lessSpringySpec<Float>().orSnap(reduceMotion)
                        )
                    }
                    launch {
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = lessSpringySpec<Float>().orSnap(reduceMotion)
                        )
                    }
                }

                LaunchedEffect(data) {
                    reset()
                }

                Box(modifier = modifier) {
                    data?.let { toastData ->
                        Box(
                            modifier = Modifier
                                .align(alignment)
                                .padding(
                                    bottom = sizeMin * 0.2f,
                                    top = 24.dp,
                                    start = 12.dp,
                                    end = 12.dp
                                )
                                .imePadding()
                                .systemBarsPadding()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    this.alpha = alpha.value
                                    translationX = offsetX.value
                                }
                                .pointerInput(toastData) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { _, drag ->
                                            scope.launch {
                                                val new = offsetX.value + drag

                                                launch {
                                                    offsetX.snapTo(
                                                        targetValue = new
                                                    )
                                                }

                                                launch {
                                                    alpha.snapTo(
                                                        targetValue = lerp(
                                                            start = 1f,
                                                            stop = 0.35f,
                                                            fraction = (abs(new) / threshold).fastCoerceIn(
                                                                0f,
                                                                1f
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            scope.launch {
                                                if (abs(offsetX.value) > threshold) {
                                                    toastData.dismiss()
                                                    reset()
                                                } else {
                                                    reset()
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            toast(toastData)
                        }
                    }
                }
            } else {
                Box(modifier = modifier) {
                    Box(modifier = Modifier.align(alignment)) {
                        data?.let { toast(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun Toast(
    data: ToastData,
    modifier: Modifier = Modifier,
    shape: Shape = ToastDefaults.shape,
    containerColor: Color = ToastDefaults.color,
    contentColor: Color = ToastDefaults.contentColor,
) {
    val screenSize = LocalScreenSize.current
    val sizeMin = screenSize.width.coerceAtMost(screenSize.height)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 0.dp, max = (sizeMin * 0.7f))
            .alpha(0.95f),
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            data.visuals.icon?.let { icon ->
                val iconContainerShape = MaterialShapes.Clover8Leaf.toShape()

                Box(
                    modifier = Modifier
                        .background(
                            color = containerColor
                                .blend(MaterialTheme.colorScheme.secondary, 0.5f)
                                .blend(MaterialTheme.colorScheme.primaryContainer, 0.05f),
                            shape = iconContainerShape
                        )
                        .clip(shape = iconContainerShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        Box(modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                style = MaterialTheme.typography.bodySmall,
                text = data.visuals.message,
                textAlign = TextAlign.Center
            )
        }
    }
}

val LocalToastHostState = compositionLocalOf { ToastHostState() }

@Stable
class ToastHostState {

    private val mutex = Mutex()

    var currentToastData by mutableStateOf<ToastData?>(null)
        private set

    suspend fun showToast(
        message: String,
        icon: ImageVector? = null,
        duration: ToastDuration = ToastDuration.Short
    ) = showToast(ToastVisualsImpl(message, icon, duration))

    suspend fun showToast(visuals: ToastVisuals) = mutex.withLock {
        try {
            suspendCancellableCoroutine { continuation ->
                currentToastData = ToastDataImpl(visuals, continuation)
            }
        } finally {
            currentToastData = null
        }
    }

    private class ToastVisualsImpl(
        override val message: String,
        override val icon: ImageVector?,
        override val duration: ToastDuration
    ) : ToastVisuals {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ToastVisualsImpl

            if (message != other.message) return false
            if (icon != other.icon) return false
            return duration == other.duration
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + icon.hashCode()
            result = 31 * result + duration.hashCode()
            return result
        }
    }

    private class ToastDataImpl(
        override val visuals: ToastVisuals,
        private val continuation: CancellableContinuation<Unit>
    ) : ToastData {

        override fun dismiss() {
            if (continuation.isActive) continuation.resume(Unit)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ToastDataImpl

            if (visuals != other.visuals) return false
            return continuation == other.continuation
        }

        override fun hashCode(): Int {
            var result = visuals.hashCode()
            result = 31 * result + continuation.hashCode()
            return result
        }
    }
}

@Stable
interface ToastData {
    val visuals: ToastVisuals
    fun dismiss()
}

@Stable
interface ToastVisuals {
    val message: String
    val icon: ImageVector?
    val duration: ToastDuration
}

sealed class ToastDuration(val time: kotlin.Long) {
    @Stable
    data object Short : ToastDuration(3500L)

    @Stable
    data object Long : ToastDuration(6500L)

    @Stable
    data class Custom(val durationMillis: kotlin.Long) : ToastDuration(durationMillis)
}

@Stable
object ToastDefaults {

    fun transition(
        reduceMotion: Boolean
    ): ContentTransform = fadeIn(
        animationSpec = tween<Float>(durationMillis = 300).orSnap(reduceMotion)
    ) + scaleIn(
        animationSpec = spring<Float>(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ).orSnap(reduceMotion),
        transformOrigin = TransformOrigin(0.5f, 1f)
    ) + slideInVertically(
        animationSpec = spring<IntOffset>(stiffness = Spring.StiffnessHigh).orSnap(reduceMotion),
        initialOffsetY = { it / 2 }
    ) togetherWith fadeOut(tween<Float>(250).orSnap(reduceMotion)) + slideOutVertically(
        animationSpec = tween<IntOffset>(500).orSnap(reduceMotion),
        targetOffsetY = { it / 2 }
    ) + scaleOut(
        animationSpec = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ).orSnap(reduceMotion),
        transformOrigin = TransformOrigin(0.5f, 1f)
    )

    val contentColor: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.inverseOnSurface.harmonizeWithPrimary()

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.inverseSurface.harmonizeWithPrimary()

    val shape get() = RoundedCornerShape(32.dp)
}

private fun ToastDuration.toMillis(
    accessibilityManager: AccessibilityManager?
): Long {
    val original = this.time
    return accessibilityManager?.calculateRecommendedTimeoutMillis(
        original,
        containsIcons = true,
        containsText = true
    ) ?: original
}

suspend fun ToastHostState.showFailureToast(
    throwable: Throwable
) = showFailureToast(
    message = throwable.extractMessage(),
    icon = if (throwable is OutOfMemoryError) {
        Icons.Outlined.Memory
    } else {
        null
    }
)

suspend fun ToastHostState.showFailureToast(
    message: String,
    icon: ImageVector? = null
) = showToast(
    message = message,
    icon = icon ?: Icons.Outlined.Error,
    duration = ToastDuration.Long
)
