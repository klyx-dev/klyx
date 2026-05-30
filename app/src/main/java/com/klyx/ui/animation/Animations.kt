@file:Suppress("NOTHING_TO_INLINE")

package com.klyx.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

inline fun <T> lessSpringySpec() = spring<T>(
    dampingRatio = 0.4f,
    stiffness = Spring.StiffnessLow
)
