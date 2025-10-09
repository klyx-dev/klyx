package com.klyx.editor.compose.scroll

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ComposeFoundationFlags.isFlingContinuationAtBoundsEnabled
import androidx.compose.foundation.ComposeFoundationFlags.isOnScrollChangedCallbackEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.dispatchOnScrollChanged
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import com.klyx.editor.compose.scroll.DragEvent.DragDelta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.absoluteValue

internal class EditorScrollNode(
    state: EditorScrollState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior?,
    orientation: Orientation,
    enabled: Boolean,
    reverseDirection: Boolean,
    interactionSource: MutableInteractionSource?,
    bringIntoViewSpec: BringIntoViewSpec?,
) :
    DragGestureNode(
        canDrag = CanDragCalculation,
        enabled = enabled,
        interactionSource = interactionSource,
        orientationLock = orientation,
    ),
    KeyInputModifierNode,
    SemanticsModifierNode,
    CompositionLocalConsumerModifierNode,
    OnScrollChangedDispatcher {

    override val shouldAutoInvalidate: Boolean = false

    private val nestedScrollDispatcher = NestedScrollDispatcher()

    private val scrollableContainerNode = delegate(ScrollableContainerNode(enabled))

    // Place holder fling behavior, we'll initialize it when the density is available.
    private val defaultFlingBehavior = platformDefaultFlingBehavior()

    private val scrollingLogic =
        ScrollingLogic(
            scrollState = state,
            orientation = orientation,
            overscrollEffect = overscrollEffect,
            reverseDirection = reverseDirection,
            flingBehavior = flingBehavior ?: defaultFlingBehavior,
            nestedScrollDispatcher = nestedScrollDispatcher,
            onScrollChangedDispatcher = this,
            isScrollableNodeAttached = { isAttached },
        )

    private val nestedScrollConnection =
        ScrollableNestedScrollConnection(enabled = enabled, scrollingLogic = scrollingLogic)

    private val contentInViewNode =
        delegate(
            ContentInViewNode(orientation, scrollingLogic, reverseDirection, bringIntoViewSpec)
        )

    private var scrollByAction: ((x: Float, y: Float) -> Boolean)? = null
    private var scrollByOffsetAction: (suspend (Offset) -> Offset)? = null

    private var mouseWheelScrollingLogic: MouseWheelScrollingLogic? = null

    init {
        /** Nested scrolling */
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))

        /** Focus scrolling */
        delegate(FocusTargetModifierNode(focusability = Focusability.Never))
        delegate(BringIntoViewResponderNode(contentInViewNode))
        delegate(FocusedBoundsObserverNode { contentInViewNode.onFocusBoundsChanged(it) })
    }

    override fun dispatchScrollDeltaInfo(delta: Offset) {
        if (!isAttached) return
        dispatchOnScrollChanged(delta)
    }

    override suspend fun drag(
        forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit
    ) {
        with(scrollingLogic) {
            scroll(scrollPriority = MutatePriority.UserInput) {
                forEachDelta {
                    scrollByWithOverscroll(it.delta.singleAxisOffset(), source = UserInput)
                }
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {}

    override fun onDragStopped(velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onScrollStopped(velocity, isMouseWheel = false)
        }
    }

    private fun onWheelScrollStopped(velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onScrollStopped(velocity, isMouseWheel = true)
        }
    }

    override fun startDragImmediately(): Boolean {
        return scrollingLogic.shouldScrollImmediately()
    }

    private fun ensureMouseWheelScrollNodeInitialized() {
        if (mouseWheelScrollingLogic == null) {
            mouseWheelScrollingLogic = MouseWheelScrollingLogic(
                scrollingLogic = scrollingLogic,
                mouseWheelScrollConfig = platformScrollConfig(),
                onScrollStopped = ::onWheelScrollStopped,
                density = requireDensity(),
            )
        }

        mouseWheelScrollingLogic?.startReceivingMouseWheelEvents(coroutineScope)
    }

    fun update(
        state: EditorScrollState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
        bringIntoViewSpec: BringIntoViewSpec?,
    ) {
        var shouldInvalidateSemantics = false
        if (this.enabled != enabled) { // enabled changed
            nestedScrollConnection.enabled = enabled
            scrollableContainerNode.update(enabled)
            shouldInvalidateSemantics = true
        }
        // a new fling behavior was set, change the resolved one.
        val resolvedFlingBehavior = flingBehavior ?: defaultFlingBehavior

        val resetPointerInputHandling =
            scrollingLogic.update(
                scrollState = state,
                orientation = orientation,
                overscrollEffect = overscrollEffect,
                reverseDirection = reverseDirection,
                flingBehavior = resolvedFlingBehavior,
                nestedScrollDispatcher = nestedScrollDispatcher,
            )
        contentInViewNode.update(orientation, reverseDirection, bringIntoViewSpec)

        this.overscrollEffect = overscrollEffect
        this.flingBehavior = flingBehavior

        // update DragGestureNode
        update(
            canDrag = CanDragCalculation,
            enabled = enabled,
            interactionSource = interactionSource,
            orientationLock = if (scrollingLogic.isVertical()) Vertical else Horizontal,
            shouldResetPointerInputHandling = resetPointerInputHandling,
        )

        if (shouldInvalidateSemantics) {
            clearScrollSemanticsActions()
            invalidateSemantics()
        }
    }

    override fun onAttach() {
        updateDefaultFlingBehavior()
        mouseWheelScrollingLogic?.updateDensity(requireDensity())
    }

    private fun updateDefaultFlingBehavior() {
        if (!isAttached) return
        val density = requireDensity()
        defaultFlingBehavior.updateDensity(density)
    }

    override fun onDensityChange() {
        onCancelPointerInput()
        updateDefaultFlingBehavior()
        mouseWheelScrollingLogic?.updateDensity(requireDensity())
    }

    // Key handler for Page up/down scrolling behavior.
    override fun onKeyEvent(event: KeyEvent): Boolean {
        return if (
            enabled &&
            (event.key == Key.PageDown || event.key == Key.PageUp) &&
            (event.type == KeyEventType.KeyDown) &&
            (!event.isCtrlPressed)
        ) {

            val scrollAmount: Offset =
                if (scrollingLogic.isVertical()) {
                    val viewportHeight = contentInViewNode.viewportSize.height

                    val yAmount =
                        if (event.key == Key.PageUp) {
                            viewportHeight.toFloat()
                        } else {
                            -viewportHeight.toFloat()
                        }

                    Offset(0f, yAmount)
                } else {
                    val viewportWidth = contentInViewNode.viewportSize.width

                    val xAmount =
                        if (event.key == Key.PageUp) {
                            viewportWidth.toFloat()
                        } else {
                            -viewportWidth.toFloat()
                        }

                    Offset(xAmount, 0f)
                }

            // A coroutine is launched for every individual scroll event in the
            // larger scroll gesture. If we see degradation in the future (that is,
            // a fast scroll gesture on a slow device causes UI jank [not seen up to
            // this point), we can switch to a more efficient solution where we
            // lazily launch one coroutine (with the first event) and use a Channel
            // to communicate the scroll amount to the UI thread.
            coroutineScope.launch {
                scrollingLogic.scroll(scrollPriority = MutatePriority.UserInput) {
                    scrollBy(offset = scrollAmount, source = UserInput)
                }
            }
            true
        } else {
            false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent) = false

    // Forward all PointerInputModifierNode method calls to `mmouseWheelScrollNode.pointerInputNode`
    // See explanation in `MouseWheelScrollNode.pointerInputNode`

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pointerEvent.changes.fastAny { canDrag.invoke(it) }) {
            super.onPointerEvent(pointerEvent, pass, bounds)
        }
        if (enabled) {
            if (pass == PointerEventPass.Initial && pointerEvent.type == PointerEventType.Scroll) {
                ensureMouseWheelScrollNodeInitialized()
            }
            mouseWheelScrollingLogic?.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (enabled && (scrollByAction == null || scrollByOffsetAction == null)) {
            setScrollSemanticsActions()
        }

        scrollByAction?.let { scrollBy(action = it) }

        scrollByOffsetAction?.let { scrollByOffset(action = it) }
    }

    private fun setScrollSemanticsActions() {
        scrollByAction = { x, y ->
            coroutineScope.launch { scrollingLogic.semanticsScrollBy(Offset(x, y)) }
            true
        }

        scrollByOffsetAction = { offset -> scrollingLogic.semanticsScrollBy(offset) }
    }

    private fun clearScrollSemanticsActions() {
        scrollByAction = null
        scrollByOffsetAction = null
    }
}

internal interface ScrollConfig {

    /** Enables animated transition of scroll on mouse wheel events. */
    val isSmoothScrollingEnabled: Boolean
        get() = true

    fun isPreciseWheelScroll(event: PointerEvent): Boolean = false

    fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset
}

internal expect fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig

// TODO: provide public way to drag by mouse (especially requested for Pager)
internal val CanDragCalculation: (PointerInputChange) -> Boolean = { change ->
    change.type != PointerType.Mouse
}

/**
 * Holds all scrolling related logic: controls nested scrolling, flinging, overscroll and delta
 * dispatching.
 */
internal class ScrollingLogic(
    var scrollState: EditorScrollState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior,
    private var orientation: Orientation,
    private var reverseDirection: Boolean,
    private var nestedScrollDispatcher: NestedScrollDispatcher,
    private var onScrollChangedDispatcher: OnScrollChangedDispatcher,
    private val isScrollableNodeAttached: () -> Boolean,
) : ScrollLogic {
    // specifies if this scrollable node is currently flinging
    override var isFlinging = false
        private set

    fun Float.toOffset(): Offset =
        when {
            this == 0f -> Offset.Zero
            orientation == Horizontal -> Offset(this, 0f)
            else -> Offset(0f, this)
        }

    fun Offset.singleAxisOffset(): Offset =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Offset.toFloat(): Float = if (orientation == Horizontal) this.x else this.y

    fun Float.toVelocity(): Velocity =
        when {
            this == 0f -> Velocity.Zero
            orientation == Horizontal -> Velocity(this, 0f)
            else -> Velocity(0f, this)
        }

    private fun Velocity.toFloat(): Float = if (orientation == Horizontal) this.x else this.y

    private fun Velocity.singleAxisVelocity(): Velocity =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    private fun Velocity.update(newValue: Float): Velocity =
        if (orientation == Horizontal) copy(x = newValue) else copy(y = newValue)

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

    private var latestScrollSource = UserInput
    private var outerStateScope = NoOpScrollScope

    private val nestedScrollScope =
        object : NestedScrollScope {
            override fun scrollBy(offset: Offset, source: NestedScrollSource): Offset {
                return with(outerStateScope) { performScroll(offset, source) }
            }

            override fun scrollByWithOverscroll(
                offset: Offset,
                source: NestedScrollSource,
            ): Offset {
                latestScrollSource = source
                val overscroll = overscrollEffect
                return if (overscroll != null && shouldDispatchOverscroll) {
                    overscroll.applyToScroll(offset, latestScrollSource, performScrollForOverscroll)
                } else {
                    with(outerStateScope) { performScroll(offset, source) }
                }
            }
        }

    private val performScrollForOverscroll: (Offset) -> Offset = { delta ->
        with(outerStateScope) { performScroll(delta, latestScrollSource) }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun EditorScrollScope.performScroll(delta: Offset, source: NestedScrollSource): Offset {
        val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(delta, source)

        val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

        val singleAxisDeltaForSelfScroll =
            scrollAvailableAfterPreScroll.singleAxisOffset().reverseIfNeeded().toFloat()

        // Consume on a single axis.
        val consumedBySelfScroll =
            scrollBy(singleAxisDeltaForSelfScroll).toOffset().reverseIfNeeded()

        // Trigger on scroll changed callback
        if (isOnScrollChangedCallbackEnabled) {
            onScrollChangedDispatcher.dispatchScrollDeltaInfo(consumedBySelfScroll)
        }

        val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
        val consumedByPostScroll =
            nestedScrollDispatcher.dispatchPostScroll(
                consumedBySelfScroll,
                deltaAvailableAfterScroll,
                source,
            )
        return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
    }

    private val shouldDispatchOverscroll
        get() = scrollState.canScrollForward || scrollState.canScrollBackward

    override fun performRawScroll(scroll: Offset): Offset {
        return if (scrollState.isScrollInProgress) {
            Offset.Zero
        } else {
            dispatchRawDelta(scroll)
        }
    }

    private fun dispatchRawDelta(scroll: Offset): Offset {
        return scrollState
            .dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
            .reverseIfNeeded()
            .toOffset()
    }

    suspend fun onScrollStopped(initialVelocity: Velocity, isMouseWheel: Boolean) {
        if (isMouseWheel && !flingBehavior.shouldBeTriggeredByMouseWheel) {
            return
        }
        val availableVelocity = initialVelocity.singleAxisVelocity()

        val performFling: suspend (Velocity) -> Velocity = { velocity ->
            val preConsumedByParent = nestedScrollDispatcher.dispatchPreFling(velocity)
            val available = velocity - preConsumedByParent

            val velocityLeft = doFlingAnimation(available)

            val consumedPost =
                nestedScrollDispatcher.dispatchPostFling((available - velocityLeft), velocityLeft)
            val totalLeft = velocityLeft - consumedPost
            velocity - totalLeft
        }

        val overscroll = overscrollEffect
        if (overscroll != null && shouldDispatchOverscroll) {
            overscroll.applyToFling(availableVelocity, performFling)
        } else {
            performFling(availableVelocity)
        }
    }

    // fling should be cancelled if we try to scroll more than we can or if this node
    // is detached during a fling.
    private fun shouldCancelFling(pixels: Float): Boolean {
        // tries to scroll forward but cannot.
        return (pixels > 0.0f && !scrollState.canScrollForward) ||
                // tries to scroll backward but cannot.
                (pixels < 0.0f && !scrollState.canScrollBackward) ||
                // node is detached.
                !isScrollableNodeAttached.invoke()
    }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        isFlinging = true
        try {
            scroll(scrollPriority = MutatePriority.Default) {
                val nestedScrollScope = this
                val reverseScope =
                    object : ScrollScope {
                        override fun scrollBy(pixels: Float): Float {
                            // Fling has hit the bounds or node left composition,
                            // cancel it to allow continuation. This will conclude this node's
                            // fling,
                            // allowing the onPostFling signal to be called
                            // with the leftover velocity from the fling animation. Any nested
                            // scroll
                            // node above will be able to pick up the left over velocity and
                            // continue
                            // the fling.
                            val cancelFling =
                                if (isFlingContinuationAtBoundsEnabled) {
                                    !isScrollableNodeAttached.invoke()
                                } else {
                                    shouldCancelFling(pixels)
                                }
                            if (pixels.absoluteValue != 0.0f && cancelFling) {
                                throw FlingCancellationException()
                            }

                            return nestedScrollScope
                                .scrollByWithOverscroll(
                                    offset = pixels.toOffset().reverseIfNeeded(),
                                    source = SideEffect,
                                )
                                .toFloat()
                                .reverseIfNeeded()
                        }
                    }
                with(reverseScope) {
                    with(flingBehavior) {
                        result =
                            result.update(
                                performFling(available.toFloat().reverseIfNeeded())
                                    .reverseIfNeeded()
                            )
                    }
                }
            }
        } finally {
            isFlinging = false
        }

        return result
    }

    fun shouldScrollImmediately(): Boolean {
        return scrollState.isScrollInProgress || overscrollEffect?.isInProgress ?: false
    }

    /** Opens a scrolling session with nested scrolling and overscroll support. */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend NestedScrollScope.() -> Unit,
    ) {
        scrollState.scroll(scrollPriority) {
            outerStateScope = this
            block.invoke(nestedScrollScope)
        }
    }

    /** @return true if the pointer input should be reset */
    fun update(
        scrollState: EditorScrollState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior,
        nestedScrollDispatcher: NestedScrollDispatcher,
    ): Boolean {
        var resetPointerInputHandling = false
        if (this.scrollState != scrollState) {
            this.scrollState = scrollState
            resetPointerInputHandling = true
        }
        this.overscrollEffect = overscrollEffect
        if (this.orientation != orientation) {
            this.orientation = orientation
            resetPointerInputHandling = true
        }
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }
        this.flingBehavior = flingBehavior
        this.nestedScrollDispatcher = nestedScrollDispatcher
        return resetPointerInputHandling
    }

    fun isVertical(): Boolean = orientation == Vertical
}

private val NoOpScrollScope: EditorScrollScope =
    object : EditorScrollScope {
        override fun scrollBy(pixels: Float): Float = pixels
    }

internal class ScrollableNestedScrollConnection(
    val scrollingLogic: ScrollLogic,
    var enabled: Boolean,
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (enabled) {
            scrollingLogic.performRawScroll(available)
        } else {
            Offset.Zero
        }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return if (enabled) {
            val velocityLeft =
                if (scrollingLogic.isFlinging) {
                    Velocity.Zero
                } else {
                    scrollingLogic.doFlingAnimation(available)
                }
            available - velocityLeft
        } else {
            Velocity.Zero
        }
    }
}

/** Interface to allow re-use across Scrollable and Scrollable2D. */
internal interface ScrollLogic {
    val isFlinging: Boolean

    fun performRawScroll(scroll: Offset): Offset

    suspend fun doFlingAnimation(available: Velocity): Velocity
}

/** Compatibility interface for default fling behaviors that depends on [Density]. */
internal interface ScrollableDefaultFlingBehavior : FlingBehavior {
    /**
     * Update the internal parameters of FlingBehavior in accordance with the new
     * [androidx.compose.ui.unit.Density] value.
     *
     * @param density new density value.
     */
    fun updateDensity(density: Density) = Unit
}

/**
 * TODO: Move it to public interface Currently, default [FlingBehavior] is not triggered at all to
 *   avoid unexpected effects during regular scrolling. However, custom one must be triggered
 *   because it's used not only for "inertia", but also for snapping in
 *   [androidx.compose.foundation.pager.Pager] or
 *   [androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior].
 */
private val FlingBehavior.shouldBeTriggeredByMouseWheel
    get() = this !is ScrollableDefaultFlingBehavior

/**
 * This method returns [ScrollableDefaultFlingBehavior] whose density will be managed by the
 * [EditorScrollElement] because it's not created inside [Composable] context.
 * This is different from [rememberPlatformDefaultFlingBehavior] which creates [FlingBehavior] whose density
 * depends on [androidx.compose.ui.platform.LocalDensity] and is automatically resolved.
 */
internal expect fun platformDefaultFlingBehavior(): ScrollableDefaultFlingBehavior

@Composable
internal expect fun rememberPlatformDefaultFlingBehavior(): FlingBehavior

internal class DefaultFlingBehavior(
    private var flingDecay: DecayAnimationSpec<Float>,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,
) : ScrollableDefaultFlingBehavior {

    // For Testing
    var lastAnimationCycleCount = 0

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        lastAnimationCycleCount = 0
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                val animationState = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
                try {
                    animationState.animateDecay(flingDecay) {
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity
                        // avoid rounding errors and stop if anything is unconsumed
                        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                        lastAnimationCycleCount++
                    }
                } catch (_: CancellationException) {
                    velocityLeft = animationState.velocity
                }
                velocityLeft
            } else {
                initialVelocity
            }
        }
    }

    override fun updateDensity(density: Density) {
        flingDecay = splineBasedDecay(density)
    }
}

private const val DefaultScrollMotionDurationScaleFactor = 1f
internal val DefaultScrollMotionDurationScale =
    object : MotionDurationScale {
        override val scaleFactor: Float
            get() = DefaultScrollMotionDurationScaleFactor
    }

/**
 * (b/311181532): This could not be flattened so we moved it to TraversableNode, but ideally
 * ScrollabeNode should be the one to be travesable.
 */
internal class ScrollableContainerNode(enabled: Boolean) : Modifier.Node(), TraversableNode {
    override val traverseKey: Any = TraverseKey

    var enabled: Boolean = enabled
        private set

    companion object TraverseKey

    fun update(enabled: Boolean) {
        this.enabled = enabled
    }
}

internal val UnityDensity = object : Density {
    override val density: Float
        get() = 1f

    override val fontScale: Float
        get() = 1f
}

/** A scroll scope for nested scrolling and overscroll support. */
internal interface NestedScrollScope {
    fun scrollBy(offset: Offset, source: NestedScrollSource): Offset

    fun scrollByWithOverscroll(offset: Offset, source: NestedScrollSource): Offset
}

/**
 * Scroll deltas originating from the semantics system. Should be dispatched as an animation driven
 * event.
 */
private suspend fun ScrollingLogic.semanticsScrollBy(offset: Offset): Offset {
    var previousValue = 0f
    scroll(scrollPriority = MutatePriority.Default) {
        animate(0f, offset.toFloat()) { currentValue, _ ->
            val delta = currentValue - previousValue
            val consumed =
                scrollBy(offset = delta.reverseIfNeeded().toOffset(), source = UserInput)
                    .toFloat()
                    .reverseIfNeeded()
            previousValue += consumed
        }
    }
    return previousValue.toOffset()
}

internal class FlingCancellationException : CancellationException("The fling animation was cancelled")

internal interface OnScrollChangedDispatcher {
    fun dispatchScrollDeltaInfo(delta: Offset)
}

