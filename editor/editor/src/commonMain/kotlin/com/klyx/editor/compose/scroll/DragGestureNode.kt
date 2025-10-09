package com.klyx.editor.compose.scroll

import androidx.compose.foundation.ComposeFoundationFlags.isAdjustPointerInputChangeOffsetForVelocityTrackerEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A node that performs drag gesture recognition and event propagation. */
internal abstract class DragGestureNode(
    canDrag: (PointerInputChange) -> Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    private var orientationLock: Orientation?,
) : DelegatingNode(), PointerInputModifierNode {

    protected var canDrag = canDrag
        private set

    protected var enabled = enabled
        private set

    protected var interactionSource = interactionSource
        private set

    // Use wrapper lambdas here to make sure that if these properties are updated while we suspend,
    // we point to the new reference when we invoke them. startDragImmediately is a lambda since we
    // need the most recent value passed to it from Scrollable.
    private val _canDrag: (PointerInputChange) -> Boolean = { this.canDrag(it) }
    private var channel: Channel<DragEvent>? = null
    private var dragInteraction: DragInteraction.Start? = null
    private var isListeningForEvents = false

    /**
     * Accumulated position offset of this [androidx.compose.ui.Modifier.Node] that happened during a drag cycle. This
     * is used to correct the pointer input events that are added to the Velocity Tracker. If this
     * Node is static during the drag cycle, nothing will happen. On the other hand, if the position
     * of this node changes during the drag cycle, we need to correct the Pointer Input used for the
     * drag events, this is because Velocity Tracker doesn't have the knowledge about changes in the
     * position of the container that uses it, and because each Pointer Input event is related to
     * the container's root. This new behavior relies on
     * [androidx.compose.foundation.ComposeFoundationFlags.isAdjustPointerInputChangeOffsetForVelocityTrackerEnabled]
     */
    private var nodeOffset = Offset.Zero

    /**
     * Responsible for the dragging behavior between the start and the end of the drag. It
     * continually invokes `forEachDelta` to process incoming events. In return, `forEachDelta`
     * calls `dragBy` method to process each individual delta.
     */
    abstract suspend fun drag(forEachDelta: suspend ((dragDelta: DragEvent.DragDelta) -> Unit) -> Unit)

    /**
     * Passes the action needed when a drag starts. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract fun onDragStarted(startedPosition: Offset)

    /**
     * Passes the action needed when a drag stops. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract fun onDragStopped(velocity: Velocity)

    /**
     * If touch slop recognition should be skipped. If this is true, this node will start
     * recognizing drag events immediately without waiting for touch slop.
     */
    abstract fun startDragImmediately(): Boolean

    private fun startListeningForEvents() {
        isListeningForEvents = true

        /**
         * To preserve the original behavior we had (before the Modifier.Node migration) we need to
         * scope the DragStopped and DragCancel methods to the node's coroutine scope instead of
         * using the one provided by the pointer input modifier, this is to ensure that even when
         * the pointer input scope is reset we will continue any coroutine scope scope that we
         * started from these methods while the pointer input scope was active.
         */
        coroutineScope.launch {
            while (isActive) {
                var event = channel?.receive()
                if (event !is DragStarted) continue
                processDragStart(event)
                try {
                    drag { processDelta ->
                        while (event !is DragStopped && event !is DragCancelled) {
                            (event as? DragDelta)?.let(processDelta)
                            event = channel?.receive()
                        }
                    }
                    if (event is DragStopped) {
                        processDragStop(event as DragStopped)
                    } else if (event is DragCancelled) {
                        processDragCancel()
                    }
                } catch (_: CancellationException) {
                    processDragCancel()
                }
            }
        }
    }

    private var pointerInputNode: SuspendingPointerInputModifierNode? = null

    override fun onDetach() {
        isListeningForEvents = false
        disposeInteractionSource()
        nodeOffset = Offset.Zero
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (enabled && pointerInputNode == null) {
            pointerInputNode = delegate(initializePointerInputNode())
        }
        pointerInputNode?.onPointerEvent(pointerEvent, pass, bounds)
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun initializePointerInputNode(): SuspendingPointerInputModifierNode {
        return SuspendingPointerInputModifierNode {
            // re-create tracker when pointer input block restarts. This lazily creates the tracker
            // only when it is need.
            val velocityTracker = VelocityTracker()
            var previousPositionOnScreen = if (isAdjustPointerInputChangeOffsetForVelocityTrackerEnabled) {
                requireLayoutCoordinates().positionOnScreen()
            } else {
                Offset.Zero
            }
            val onDragStart:
                        (
                down: PointerInputChange,
                slopTriggerChange: PointerInputChange,
                postSlopOffset: Offset,
            ) -> Unit =
                { down, slopTriggerChange, postSlopOffset ->
                    nodeOffset = Offset.Zero // restart node offset
                    if (canDrag.invoke(down)) {
                        if (!isListeningForEvents) {
                            if (channel == null) {
                                channel = Channel(capacity = Channel.UNLIMITED)
                            }
                            startListeningForEvents()
                        }
                        velocityTracker.addPointerInputChange(down)
                        val dragStartedOffset = slopTriggerChange.position - postSlopOffset
                        // the drag start event offset is the down event + touch slop value
                        // or in this case the event that triggered the touch slop minus
                        // the post slop offset
                        channel?.trySend(DragEvent.DragStarted(dragStartedOffset))
                    }
                }

            val onDragEnd: (change: PointerInputChange) -> Unit = { upEvent ->
                velocityTracker.addPointerInputChange(upEvent)
                val maximumVelocity = viewConfiguration.maximumFlingVelocity
                val velocity =
                    velocityTracker.calculateVelocity(Velocity(maximumVelocity, maximumVelocity))
                velocityTracker.resetTracking()
                channel?.trySend(DragEvent.DragStopped(velocity.toValidVelocity()))
            }

            val onDragCancel: () -> Unit = { channel?.trySend(DragCancelled) }

            val shouldAwaitTouchSlop: () -> Boolean = { !startDragImmediately() }

            val onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit =
                { change, delta ->
                    if (isAdjustPointerInputChangeOffsetForVelocityTrackerEnabled) {
                        val currentPositionOnScreen = requireLayoutCoordinates().positionOnScreen()
                        // container changed positions
                        if (currentPositionOnScreen != previousPositionOnScreen) {
                            val delta = currentPositionOnScreen - previousPositionOnScreen
                            nodeOffset += delta
                        }
                        previousPositionOnScreen = currentPositionOnScreen
                    }
                    velocityTracker.addPointerInputChange(event = change, offset = nodeOffset)
                    channel?.trySend(DragEvent.DragDelta(delta))
                }

            coroutineScope {
                try {
                    detectDragGestures(
                        orientationLock = orientationLock,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        shouldAwaitTouchSlop = shouldAwaitTouchSlop,
                        onDrag = onDrag,
                    )
                } catch (cancellation: CancellationException) {
                    channel?.trySend(DragCancelled)
                    if (!isActive) throw cancellation
                }
            }
        }
    }

    override fun onCancelPointerInput() {
        pointerInputNode?.onCancelPointerInput()
    }

    private suspend fun processDragStart(event: DragEvent.DragStarted) {
        dragInteraction?.let { oldInteraction ->
            interactionSource?.emit(DragInteraction.Cancel(oldInteraction))
        }
        val interaction = DragInteraction.Start()
        interactionSource?.emit(interaction)
        dragInteraction = interaction
        onDragStarted(event.startPoint)
    }

    private suspend fun processDragStop(event: DragEvent.DragStopped) {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Stop(interaction))
            dragInteraction = null
        }
        onDragStopped(event.velocity)
    }

    private suspend fun processDragCancel() {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
        onDragStopped(Velocity.Zero)
    }

    fun disposeInteractionSource() {
        dragInteraction?.let { interaction ->
            interactionSource?.tryEmit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
    }

    fun update(
        canDrag: (PointerInputChange) -> Boolean = this.canDrag,
        enabled: Boolean = this.enabled,
        interactionSource: MutableInteractionSource? = this.interactionSource,
        orientationLock: Orientation? = this.orientationLock,
        shouldResetPointerInputHandling: Boolean = false,
    ) {
        var resetPointerInputHandling = shouldResetPointerInputHandling

        this.canDrag = canDrag
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) {
                disposeInteractionSource()
                pointerInputNode?.let { undelegate(it) }
                pointerInputNode = null
            }
            resetPointerInputHandling = true
        }
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }

        if (this.orientationLock != orientationLock) {
            this.orientationLock = orientationLock
            resetPointerInputHandling = true
        }

        if (resetPointerInputHandling) {
            pointerInputNode?.resetPointerInputHandler()
        }
    }
}

internal sealed class DragEvent {
    class DragStarted(val startPoint: Offset) : DragEvent()

    class DragStopped(val velocity: Velocity) : DragEvent()

    object DragCancelled : DragEvent()

    class DragDelta(val delta: Offset) : DragEvent()
}

private fun Offset.toFloat(orientation: Orientation) =
    if (orientation == Vertical) this.y else this.x

private fun Velocity.toFloat(orientation: Orientation) =
    if (orientation == Vertical) this.y else this.x

private fun Velocity.toValidVelocity() =
    Velocity(if (this.x.isNaN()) 0f else this.x, if (this.y.isNaN()) 0f else this.y)

private val NoOpOnDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {}
private val NoOpOnDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = {}
