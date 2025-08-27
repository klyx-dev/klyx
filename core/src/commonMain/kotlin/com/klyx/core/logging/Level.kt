package com.klyx.core.logging

import androidx.compose.ui.graphics.Color

enum class Level(val priority: Int) {
    Verbose(0),
    Debug(1),
    Info(2),
    Warning(3),
    Error(4),
    Assert(5);

    val displayName get() = name
}

val Level.color: Color
    get() = when (this) {
        Level.Verbose -> Color(0xFF9E9E9E)
        Level.Debug -> Color(0xFF2196F3)
        Level.Info -> Color(0xFF4CAF50)
        Level.Warning -> Color(0xFFFFC107)
        Level.Error -> Color(0xFFF44336)
        Level.Assert -> Color(0xFF9C27B0)
    }

val Level.backgroundColor: Color
    get() = when (this) {
        Level.Verbose -> Color(0xFF9E9E9E).copy(alpha = 0.15f)
        Level.Debug -> Color(0xFF2196F3).copy(alpha = 0.15f)
        Level.Info -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        Level.Warning -> Color(0xFFFFC107).copy(alpha = 0.20f) // a bit stronger for visibility
        Level.Error -> Color(0xFFF44336).copy(alpha = 0.20f)
        Level.Assert -> Color(0xFF9C27B0).copy(alpha = 0.15f)
    }
