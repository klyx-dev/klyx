package com.klyx.nodegraph

import androidx.compose.ui.geometry.Offset

val NodeData.execOut: NodePin
    get() = pins.first { it.type == PinType.Flow && it.direction == PinDirection.Output }

val NodeData.execIn: NodePin
    get() = pins.first { it.type == PinType.Flow && it.direction == PinDirection.Input }

fun NodeData.execOut(label: String) = pins.first {
    it.type == PinType.Flow && it.direction == PinDirection.Output && it.label == label
}

fun NodeData.execIn(label: String) = pins.first {
    it.type == PinType.Flow && it.direction == PinDirection.Input && it.label == label
}

fun NodeData.output(type: PinType) = pins.first { it.type == type && it.direction == PinDirection.Output }
fun NodeData.input(type: PinType) = pins.first { it.type == type && it.direction == PinDirection.Input }

fun NodeData.input(name: String) = pins.first { it.direction == PinDirection.Input && it.label == name }
fun NodeData.output(name: String) = pins.first { it.direction == PinDirection.Output && it.label == name }

fun GraphState.connect(from: NodePin, to: NodePin) {
    addConnection(from.id, to.id)
}

fun NodeData.offsetBy(x: Float = 0f, y: Float = 0f): Offset {
    return Offset(this.position.x + x, this.position.y + y)
}

private const val EST_NODE_WIDTH = 250f
private const val EST_NODE_HEIGHT = 150f

fun NodeData.rightOf(gap: Float = 50f, yOffset: Float = 0f, myWidth: Float = EST_NODE_WIDTH): Offset {
    return Offset(this.position.x + myWidth + gap, this.position.y + yOffset)
}

/**
 * Places a node to the left.
 * @param gap The empty space between the left edge of this node and the right edge of the new node.
 */
fun NodeData.leftOf(gap: Float = 50f, yOffset: Float = 0f, newNodeWidth: Float = EST_NODE_WIDTH): Offset {
    return Offset(this.position.x - gap - newNodeWidth, this.position.y + yOffset)
}

/**
 * Places a node below.
 * @param gap The empty space between the bottom of this node and the top of the new node.
 */
fun NodeData.below(gap: Float = 50f, xOffset: Float = 0f, myHeight: Float = EST_NODE_HEIGHT): Offset {
    return Offset(this.position.x + xOffset, this.position.y + myHeight + gap)
}

/**
 * Places a node above.
 * @param gap The empty space between the top of this node and the bottom of the new node.
 */
fun NodeData.above(gap: Float = 50f, xOffset: Float = 0f, newNodeHeight: Float = EST_NODE_HEIGHT): Offset {
    return Offset(this.position.x + xOffset, this.position.y - gap - newNodeHeight)
}
