package com.klyx

import java.io.File

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val os: String = System.getProperty("os.name")
    override val architecture: String = System.getProperty("os.arch")
}

actual fun platform(): Platform = JVMPlatform()
actual val fileSeparatorChar: Char get() = File.separatorChar
