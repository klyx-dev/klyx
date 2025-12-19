package com.klyx.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec

@OptIn(ExperimentalForeignApi::class)
actual fun systemNanoTime() = memScoped {
    val ts = alloc<timespec>()
    if (clock_gettime(CLOCK_MONOTONIC.toUInt(), ts.ptr) != 0) {
        error("clock_gettime failed")
    } else {
        ts.tv_sec * 1_000_000_000 + ts.tv_nsec
    }
}
