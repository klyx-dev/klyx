package com.klyx.core

actual fun App.isDebugMode(): Boolean = System.getenv("KLYX_DEBUG") == "1" || App::class.java.desiredAssertionStatus()
