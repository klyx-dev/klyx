package com.klyx.core.platform

import kotlin.jvm.JvmInline

expect fun currentOs(): Os
expect fun currentArchitecture(): Architecture

data class Platform(val os: Os, val arch: Architecture)

fun currentPlatform() = Platform(currentOs(), currentArchitecture())

@JvmInline
value class Os private constructor(val value: Int) {
    override fun toString(): String {
        return when (this) {
            Mac -> "Mac"
            Linux -> "Linux"
            Windows -> "Windows"
            Android -> "Android"
            iOS -> "iOS"
            else -> "Unknown"
        }
    }

    companion object {
        val Mac = Os(0)
        val Linux = Os(1)
        val Windows = Os(2)
        val Android = Os(3)
        val iOS = Os(4)
    }
}

@JvmInline
value class Architecture private constructor(val value: Int) {
    override fun toString(): String {
        return when (this) {
            Aarch64 -> "Aarch64"
            X86 -> "X86"
            X8664 -> "X86_64"
            else -> "Unknown"
        }
    }

    companion object {
        val Aarch64 = Architecture(0)
        val X86 = Architecture(1)
        val X8664 = Architecture(2)
    }
}
