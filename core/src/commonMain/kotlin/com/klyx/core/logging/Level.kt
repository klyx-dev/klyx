package com.klyx.core.logging

enum class Level(val priority: Int) {
    Verbose(0),
    Debug(1),
    Info(2),
    Warn(3),
    Error(4),
    Assert(5)
}
