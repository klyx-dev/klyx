@file:OptIn(ExperimentalForeignApi::class)

package com.klyx.runtime

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

actual val currentPlatform = Platform.iOS

actual fun getenv(name: String): String? = getenv(name)?.toKString()

actual val systemPathSeparator = ':'
