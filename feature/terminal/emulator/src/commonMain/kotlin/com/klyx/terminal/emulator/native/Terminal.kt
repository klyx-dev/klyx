package com.klyx.terminal.emulator.native

internal expect object Terminal {
    fun createSubprocess(
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

    fun setPtyWindowSize(fd: Int, rows: Int, cols: Int, cellWidth: Int, cellHeight: Int)
    fun setPtyUTF8Mode(fd: Int)
    fun waitFor(pid: Int): Int
    fun close(fd: Int)
    fun readFromFd(fd: Int, buffer: ByteArray, maxLen: Int): Int
    fun writeToFd(fd: Int, buffer: ByteArray, len: Int): Int
    fun readSymlink(path: String): String?
    fun killProcess(pid: Int, signal: Int): Int
}
