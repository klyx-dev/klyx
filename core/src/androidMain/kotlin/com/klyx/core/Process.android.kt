package com.klyx.core

import android.os.Process

actual object Process {
    actual fun is64Bit() = Process.is64Bit()
}

