package com.klyx.terminal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.PlatformContext
import com.klyx.core.app.Global
import kotlinx.coroutines.flow.StateFlow

@Stable
interface SessionBinder : Global {
    val isBounded: StateFlow<Boolean>

    fun bind(context: PlatformContext)

    fun unbind(context: PlatformContext)
}

expect fun SessionBinder(): SessionBinder

val LocalSessionBinder = staticCompositionLocalOf { SessionBinder() }
