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

expect fun platform(): Platform
