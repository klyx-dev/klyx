package com.klyx.api.platform

import android.os.Build

/**
 * Represents the CPU architecture of the device.
 *
 * This value class provides a type-safe way to handle different processor architectures
 * supported by Android, such as ARM64, x86_64, etc.
 */
@JvmInline
value class Architecture private constructor(val value: Int) {

    /**
     * Returns a lowercase string representation of the architecture (e.g., "aarch64", "x86_64").
     */
    override fun toString(): String {
        return when (this) {
            Aarch64 -> "aarch64"
            Arm -> "arm"
            X86 -> "x86"
            X86_64 -> "x86_64"
            else -> "unknown"
        }
    }

    companion object {

        /** 64-bit ARM architecture (ARMv8 or later). */
        val Aarch64 = Architecture(0)

        /** 32-bit ARM architecture. */
        val Arm = Architecture(1)

        /** 32-bit x86 architecture. */
        val X86 = Architecture(2)

        /** 64-bit x86 architecture. */
        val X86_64 = Architecture(3)

        /** Architecture could not be determined. */
        val Unknown = Architecture(-1)
    }
}

/**
 * Detects and returns the primary [Architecture] of the current device.
 *
 * It uses [Build.SUPPORTED_ABIS] to determine the most preferred
 * architecture supported by the device runtime.
 */
fun currentArchitecture(): Architecture {
    val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: return Architecture.Unknown

    return when {
        "aarch64" in arch || "arm64" in arch -> Architecture.Aarch64
        "arm" in arch -> Architecture.Arm
        "x86_64" in arch || "amd64" in arch -> Architecture.X86_64
        "x86" in arch || arch.startsWith("i") -> Architecture.X86
        else -> Architecture.Unknown
    }
}
