package com.klyx.terminal.native

import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative

object Native {
    init {
        System.loadLibrary("terminal")
    }

    @JvmStatic
    @FastNative
    external fun createSubprocess(
        cmd: String,
        cwd: String,
        args: Array<String>,
        envVars: Array<String>,
        processIdArray: IntArray,
        rows: Int,
        columns: Int,
        cellWidth: Int,
        cellHeight: Int
    ): Int

    @JvmStatic
    @CriticalNative
    external fun setPtyWindowSize(fd: Int, rows: Int, cols: Int, cellWidth: Int, cellHeight: Int)

    @JvmStatic
    @CriticalNative
    external fun setPtyUTF8Mode(fd: Int)

    @JvmStatic
    external fun waitFor(pid: Int): Int

    @JvmStatic
    @CriticalNative
    external fun close(fd: Int)
}
