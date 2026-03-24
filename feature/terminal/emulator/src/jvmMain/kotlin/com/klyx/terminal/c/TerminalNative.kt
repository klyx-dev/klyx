package com.klyx.terminal.c

object TerminalNative {
    init {
        System.loadLibrary("terminal")
    }

    @JvmStatic
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
    external fun setPtyWindowSize(fd: Int, rows: Int, cols: Int, cellWidth: Int, cellHeight: Int)

    @JvmStatic
    external fun setPtyUTF8Mode(fd: Int)

    @JvmStatic
    external fun waitFor(pid: Int): Int

    @JvmStatic
    external fun close(fd: Int)

    @JvmStatic
    external fun readFromFd(fd: Int, buffer: ByteArray, maxLen: Int): Int

    @JvmStatic
    external fun writeToFd(fd: Int, buffer: ByteArray, len: Int): Int

    @JvmStatic
    external fun readSymlink(path: String): String?

    @JvmStatic
    external fun killProcess(pid: Int, signal: Int): Int
}
