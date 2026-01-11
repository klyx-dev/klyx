package com.klyx.core.platform

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 * A [CoroutineScope] designated for background operations.
 */
typealias BackgroundScope = CoroutineScope

/**
 * A [CoroutineScope] intended for UI-bound or main-thread operations.
 */
typealias ForegroundScope = CoroutineScope

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

    /**
     * A [CoroutineScope] designated for background operations.
     *
     * This scope uses [Dispatchers.Default] for executing CPU-bound tasks and includes a [SupervisorJob],
     * ensuring that the failure of one child coroutine does not cancel the entire scope or other siblings.
     */
    val backgroundScope: BackgroundScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * A [CoroutineScope] intended for UI-bound or main-thread operations.
     *
     * This scope uses [Dispatchers.Main] and is supervised by a [SupervisorJob],
     * meaning failure of one child coroutine will not cancel the others.
     */
    val foregroundScope: ForegroundScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        /**
         * Retrieves the [Platform] instance representing the current runtime environment.
         *
         * @see currentPlatform
         */
        inline val Current get() = currentPlatform()
    }
}

/**
 * Gracefully quit the application via the platform's standard routine.
 */
expect fun Platform.quit(): Nothing

/**
 * A [CompositionLocal] that provides the current [Platform] to the Compose hierarchy.
 *
 * @see Platform
 * @see currentPlatform
 */
val LocalPlatform = staticCompositionLocalOf { currentPlatform() }

/**
 * A [CompositionLocal] that provides the current operating system ([Os])
 * to the Compose hierarchy.
 *
 * @see Os
 * @see currentOs
 * @see LocalPlatform
 */
val LocalOs = staticCompositionLocalOf { currentOs() }

/**
 * A [CompositionLocal] that provides the current CPU [Architecture]
 * to the Compose hierarchy.
 *
 * @see Architecture
 * @see currentArchitecture
 * @see LocalPlatform
 */
val LocalArchitecture = staticCompositionLocalOf { currentArchitecture() }

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

val ExeSuffix = if (currentOs() == Os.Windows) ".exe" else ""
