package com.klyx

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val os: String = System.getProperty("os.name")
}

actual fun platform(): Platform = JVMPlatform()
