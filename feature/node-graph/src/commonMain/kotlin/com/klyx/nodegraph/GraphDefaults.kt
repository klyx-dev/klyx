package com.klyx.nodegraph

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object GraphDefaults {
    val PinDotSize = 14.dp

    @Stable
    fun settings(
        deleteWireOnLongPress: Boolean = true,
        rerouteWireOnDoubleTap: Boolean = true,
        showCompatibleNodesOnReleaseOnEmptyCanvas: Boolean = true,
        showMinimap: Boolean = false,
        backgroundType: GraphBackgroundType = GraphBackgroundType.Dot
    ): GraphSettings = GraphSettings(
        deleteWireOnLongPress = deleteWireOnLongPress,
        rerouteWireOnDoubleTap = rerouteWireOnDoubleTap,
        showCompatibleNodesOnReleaseOnEmptyCanvas = showCompatibleNodesOnReleaseOnEmptyCanvas,
        showMinimap = showMinimap,
        backgroundType = backgroundType
    )

    @Stable
    fun colors(
        graphBackgroundColor: Color = Color(0xFF0F0F1A),
        nodeBackgroundColor: Color = Color(0xFF1C1C2E),
        nodeOutlineColor: Color = Color(0xFF3A3A55),
        nodeSelectionBorderColor: Color = Color(0xFF4FC3F7),
        labelColor: Color = Color(0xFFBBBBCC),
        titleColor: Color = Color.White,
        panelBackgroundColor: Color = Color(0xF0111122)
    ): GraphColors = GraphColors(
        graphBackgroundColor = graphBackgroundColor,
        nodeBackgroundColor = nodeBackgroundColor,
        nodeOutlineColor = nodeOutlineColor,
        nodeSelectionBorderColor = nodeSelectionBorderColor,
        labelColor = labelColor,
        titleColor = titleColor,
        panelBackgroundColor = panelBackgroundColor
    )
}

@Immutable
data class GraphColors(
    val graphBackgroundColor: Color,
    val nodeBackgroundColor: Color,
    val nodeOutlineColor: Color,
    val nodeSelectionBorderColor: Color,
    val labelColor: Color,
    val titleColor: Color,
    val panelBackgroundColor: Color
)
