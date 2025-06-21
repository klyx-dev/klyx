package com.klyx.core

sealed class BuildVariant {
    data object Debug : BuildVariant()
    data object Release : BuildVariant()
}

val BuildVariant.isRelease get() = this == BuildVariant.Release
val BuildVariant.isDebug get() = this == BuildVariant.Debug

