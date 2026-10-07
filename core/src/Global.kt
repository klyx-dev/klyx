package com.klyx.core

import kotlin.reflect.KType

/** Marker for a piece of app-wide state. Implementations are stored in the app's Koin graph. */
interface Global

class NoGlobalException(
    message: String,
    val type: KType? = null,
    override val cause: Throwable? = null
) : RuntimeException(message)
