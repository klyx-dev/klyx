package com.klyx.ui.provider

import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.event.EventBus

val LocalEventBus = staticCompositionLocalOf<EventBus> {
    error("No LocalEventBus provided")
}
