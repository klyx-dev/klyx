@file:Suppress("MatchingDeclarationName")

package com.klyx

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as KPlatform

@OptIn(ExperimentalNativeApi::class)
class NativePlatform : Platform {
    override val name = "Native"
    override val os = KPlatform.osFamily.name
    override val architecture = KPlatform.cpuArchitecture.name
}

actual fun platform(): Platform = NativePlatform()

actual val lineSeparator = "\n"

actual val fileSeparatorChar = '/'
