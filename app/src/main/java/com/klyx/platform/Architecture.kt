package com.klyx.platform

import android.os.Build

/**
 * Represents a CPU architecture.
 *
 * The available architectures are:
 * - `Aarch64`: Represents the AArch64 (ARM 64-bit) architecture.
 * - `Arm`: Represents the ARM (32-bit) architecture.
 * - `X86`: Represents the x86 (32-bit) architecture.
 * - `X8664`: Represents the x86_64 (64-bit) architecture.
 */
@JvmInline
value class Architecture private constructor(val value: Int) {
    override fun toString(): String {
        return when (this) {
            Aarch64 -> "aarch64"
            Arm -> "arm"
            X86 -> "x86"
            X8664 -> "x86_64"
            else -> "unknown"
        }
    }

    companion object {
        val Aarch64 = Architecture(0)
        val Arm = Architecture(1)
        val X86 = Architecture(2)
        val X8664 = Architecture(3)
        val Unknown = Architecture(-1)
    }
}

fun currentArchitecture(): Architecture {
    val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: return Architecture.Unknown

    return when {
        "aarch64" in arch || "arm64" in arch -> Architecture.Aarch64
        "arm" in arch -> Architecture.Arm
        "x86_64" in arch || "amd64" in arch -> Architecture.X8664
        "x86" in arch || arch.startsWith("i") -> Architecture.X86
        else -> Architecture.Unknown
    }
}
