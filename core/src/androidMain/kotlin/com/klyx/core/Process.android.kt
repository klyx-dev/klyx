package com.klyx.core

import android.os.Process

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object Process {
    actual fun is64Bit() = Process.is64Bit()
}

