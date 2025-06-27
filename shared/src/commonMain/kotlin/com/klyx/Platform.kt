package com.klyx

import oshi.PlatformEnum
import oshi.SystemInfo

interface Platform {
    val name: String
    val os: String
    val architecture: String
}

private val systemInfo by lazy { SystemInfo() }

val Platform.isAndroid get() = SystemInfo.getCurrentPlatform() == PlatformEnum.ANDROID
val Platform.isWindows get() = SystemInfo.getCurrentPlatform() == PlatformEnum.WINDOWS

val lineSeparator: String = System.lineSeparator()

expect val fileSeparatorChar: Char

expect fun platform(): Platform
