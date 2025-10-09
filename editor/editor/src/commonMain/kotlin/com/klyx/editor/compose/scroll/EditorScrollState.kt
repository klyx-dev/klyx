package com.klyx.editor.compose.scroll

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope

interface EditorScrollState {
    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [EditorScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this object)
     * in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere with the [scrollPriority] higher or equal to ongoing
     * scroll, ongoing scroll will be canceled.
     */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend EditorScrollScope.() -> Unit,
    )

    /**
     * Dispatch scroll delta in pixels avoiding all scroll related mechanisms.
     *
     * **NOTE:** unlike [scroll], dispatching any delta with this method won't trigger nested
     * scroll, won't stop ongoing scroll/drag animation and will bypass scrolling of any priority.
     * This method will also ignore `reverseDirection` and other parameters set in scrollable.
     *
     * This method is used internally for nested scrolling dispatch and other low level operations,
     * allowing implementers of [EditorScrollState] influence the consumption as suits them. Manually
     * dispatching delta via this method will likely result in a bad user experience, you must
     * prefer [scroll] method over this one.
     *
     * @param delta amount of scroll dispatched in the nested scroll process
     * @return the amount of delta consumed
     */
    fun dispatchRawDelta(delta: Float): Float

    /**
     * Whether this [EditorScrollState] is currently scrolling by gesture, fling or programmatically
     * or not.
     */
    val isScrollInProgress: Boolean

    /**
     * Whether this [EditorScrollState] can scroll forward (consume a positive delta). This is
     * typically false if the scroll position is equal to its maximum value, and true otherwise.
     *
     * Note that `true` here does not imply that delta *will* be consumed - the EditorScrollState may
     * decide not to handle the incoming delta (such as if it is already being scrolled separately).
     * Additionally, for backwards compatibility with previous versions of EditorScrollState this
     * value defaults to `true`.
     */
    val canScrollForward: Boolean
        get() = true

    /**
     * Whether this [EditorScrollState] can scroll backward (consume a negative delta). This is
     * typically false if the scroll position is equal to its minimum value, and true otherwise.
     *
     * Note that `true` here does not imply that delta *will* be consumed - the EditorScrollState may
     * decide not to handle the incoming delta (such as if it is already being scrolled separately).
     * Additionally, for backwards compatibility with previous versions of EditorScrollState this
     * value defaults to `true`.
     */
    val canScrollBackward: Boolean
        get() = true

    /**
     * The value of this property is true under the following scenarios, otherwise it's false.
     * - This [EditorScrollState] is currently scrolling forward.
     * - This [EditorScrollState] was scrolling forward in its last scroll action.
     */
    @get:Suppress("GetterSetterNames")
    val lastScrolledForward: Boolean
        get() = false

    /**
     * The value of this property is true under the following scenarios, otherwise it's false.
     * - This [EditorScrollState] is currently scrolling backward.
     * - This [EditorScrollState] was scrolling backward in its last scroll action.
     */
    @get:Suppress("GetterSetterNames")
    val lastScrolledBackward: Boolean
        get() = false
}

/**
 * @param consumeScrollDelta callback invoked when drag/fling/smooth scrolling occurs. The callback
 *   receives the delta in pixels. Callers should update their state in this lambda and return the
 *   amount of delta consumed
 */
fun EditorScrollState(consumeScrollDelta: (Float) -> Float): EditorScrollState {
    return DefaultEditorScrollState(consumeScrollDelta)
}

/**
 * @param consumeScrollDelta callback invoked when drag/fling/smooth scrolling occurs. The callback
 *   receives the delta in pixels. Callers should update their state in this lambda and return the
 *   amount of delta consumed
 */
@Composable
fun rememberEditorScrollState(consumeScrollDelta: (Float) -> Float): EditorScrollState {
    val lambdaState = rememberUpdatedState(consumeScrollDelta)
    return remember { EditorScrollState { lambdaState.value.invoke(it) } }
}

/** Scope used for suspending scroll blocks */
interface EditorScrollScope {
    /**
     * Attempts to scroll forward by [pixels] px.
     *
     * @return the amount of the requested scroll that was consumed (that is, how far it scrolled)
     */
    fun scrollBy(pixels: Float): Float
}

private class DefaultEditorScrollState(val onDelta: (Float) -> Float) : EditorScrollState {
    private val scrollScope: EditorScrollScope =
        object : EditorScrollScope {
            override fun scrollBy(pixels: Float): Float {
                if (pixels.isNaN()) return 0f
                val delta = onDelta(pixels)
                isLastScrollForwardState.value = delta > 0
                isLastScrollBackwardState.value = delta < 0
                return delta
            }
        }

    private val scrollMutex = MutatorMutex()

    private val isScrollingState = mutableStateOf(false)
    private val isLastScrollForwardState = mutableStateOf(false)
    private val isLastScrollBackwardState = mutableStateOf(false)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend EditorScrollScope.() -> Unit,
    ): Unit = coroutineScope {
        scrollMutex.mutateWith(scrollScope, scrollPriority) {
            isScrollingState.value = true
            try {
                block()
            } finally {
                isScrollingState.value = false
            }
        }
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return onDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value

    override val lastScrolledForward: Boolean
        get() = isLastScrollForwardState.value

    override val lastScrolledBackward: Boolean
        get() = isLastScrollBackwardState.value
}

