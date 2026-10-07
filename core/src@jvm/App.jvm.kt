package com.klyx.core

import okio.FileSystem

actual fun App.isDebugMode(): Boolean = System.getenv("KLYX_DEBUG") == "1" || App::class.java.desiredAssertionStatus()

actual val PlatformFileSystem = FileSystem.SYSTEM
