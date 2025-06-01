package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.klyx.BuildConfig

sealed class BuildVariant {
    data object Debug : BuildVariant()

    data object Release : BuildVariant()
}

val BuildVariant.isDebug get() = this == BuildVariant.Debug

val BuildVariant.isRelease get() = this == BuildVariant.Release

@Composable
fun rememberBuildVariant() = remember {
    if (BuildConfig.DEBUG) BuildVariant.Debug else BuildVariant.Release
}
