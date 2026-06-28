package com.klyx.api.platform

import android.os.Build

@JvmInline
value class Architecture private constructor(val value: Int) {
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
        val Aarch64 = Architecture(0)
        val Arm = Architecture(1)
        val X86 = Architecture(2)
        val X86_64 = Architecture(3)
        val Unknown = Architecture(-1)
    }
}

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
