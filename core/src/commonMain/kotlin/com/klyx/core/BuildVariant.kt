package com.klyx.core

sealed class BuildVariant {
    data object Debug : BuildVariant()
    data object Release : BuildVariant()
}

inline val BuildVariant.isRelease get() = this == BuildVariant.Release
inline val BuildVariant.isDebug get() = this == BuildVariant.Debug
