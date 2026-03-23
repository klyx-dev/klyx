package com.klyx.nodegraph

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class GraphSettings(
    val deleteWireOnLongPress: Boolean,
    val rerouteWireOnDoubleTap: Boolean,
    val showCompatibleNodesOnReleaseOnEmptyCanvas: Boolean,
    val showMinimap: Boolean,
    val backgroundType: GraphBackgroundType
)

@Serializable
enum class GraphBackgroundType {
    Dot, Lines
}
