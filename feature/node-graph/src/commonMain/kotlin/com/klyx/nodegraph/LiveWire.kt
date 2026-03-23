package com.klyx.nodegraph

import androidx.compose.ui.geometry.Offset
import kotlin.uuid.Uuid

internal data class LiveWire(
    val anchorPinId: Uuid,
    val anchorIsOutput: Boolean,
    val tipGraphPos: Offset,
)
