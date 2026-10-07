package com.klyx.core

import okio.FileSystem
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual fun App.isDebugMode(): Boolean = Platform.isDebugBinary

actual val PlatformFileSystem = FileSystem.SYSTEM
