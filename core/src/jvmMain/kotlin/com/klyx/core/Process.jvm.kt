package com.klyx.core

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object Process {
    actual fun is64Bit(): Boolean {
        val arch = System.getProperty("os.arch")?.lowercase()
        return arch != null && (arch.contains("64") || arch == "aarch64")
    }
}
