package com.klyx.ui.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

val LocalReduceMotion = compositionLocalOf { false }

/**
 * Accessor for the current value of [LocalReduceMotion].
 */
val ReduceMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReduceMotion.current

/**
 * Returns a [snap] animation if the user has requested
 * reduced motion, otherwise defaults cleanly back to your premium spring/tween.
 */
fun <T> FiniteAnimationSpec<T>.orSnap(reduceMotion: Boolean): FiniteAnimationSpec<T> {
    return if (reduceMotion) snap() else this
}

/**
 * Returns a [snap] animation if [LocalReduceMotion] is enabled.
 */
@Composable
@ReadOnlyComposable
fun <T> FiniteAnimationSpec<T>.orSnap() = orSnap(ReduceMotion)
