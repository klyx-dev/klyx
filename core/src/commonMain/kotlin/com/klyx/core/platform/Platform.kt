package com.klyx.core.platform

import kotlin.jvm.JvmInline

/**
 * Detects the operating system on which the code is currently running.
 *
 * @return An instance of [Os] representing the identified operating system.
 */
expect fun currentOs(): Os

/**
 * Retrieves the current architecture of the underlying system.
 *
 * @return An [Architecture] object representing the CPU architecture
 *         of the current runtime environment.
 */
expect fun currentArchitecture(): Architecture

/**
 * Represents a platform defined by an operating system and an architecture.
 *
 * This class is used to identify the specific combination of OS and architecture
 * on which the application is running.
 *
 * @property os The operating system of the platform.
 * @property arch The architecture of the platform.
 */
data class Platform(val os: Os, val arch: Architecture) {
    override fun toString(): String {
        return "$os ($arch)"
    }

    companion object {
        inline val Current get() = currentPlatform()
    }
}

/**
 * Determines the platform on which the application is currently running.
 *
 * @return A `Platform` object containing the operating system and architecture
 *         of the current runtime environment.
 *
 * @see currentOs
 * @see currentArchitecture
 */
fun currentPlatform() = Platform(currentOs(), currentArchitecture())

/**
 * Represents an operating system.
 *
 * @author Vivek
 */
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
        /**
         * The macOS operating system.
         */
        val Mac = Os(0)

        /**
         * The Linux operating system.
         */
        val Linux = Os(1)

        /**
         * The Microsoft Windows operating system.
         */
        val Windows = Os(2)

        /**
         * The Android operating system.
         */
        val Android = Os(3)

        /**
         * The `iOS` operating system.
         */
        val iOS = Os(4)
    }
}

/**
 * Represents a CPU architecture.
 *
 * The available architectures are:
 * - `Aarch64`: Represents the AArch64 (ARM 64-bit) architecture.
 * - `X86`: Represents the x86 (32-bit) architecture.
 * - `X8664`: Represents the x86_64 (64-bit) architecture.
 */
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
