package com.klyx.terminal.emulator.native

import com.klyx.terminal.c.TerminalNative

internal actual object Terminal {
    actual fun createSubprocess(
        cmd: String,
        cwd: String,
        args: Array<String>,
        envVars: Array<String>,
        processIdArray: IntArray,
        rows: Int,
        columns: Int,
        cellWidth: Int,
        cellHeight: Int
    ): Int {
        return TerminalNative.createSubprocess(
            cmd = cmd,
            cwd = cwd,
            args = args,
            envVars = envVars,
            processIdArray = processIdArray,
            rows = rows,
            columns = columns,
            cellWidth = cellWidth,
            cellHeight = cellHeight
        )
    }

    actual fun setPtyWindowSize(
        fd: Int,
        rows: Int,
        cols: Int,
        cellWidth: Int,
        cellHeight: Int
    ) {
        TerminalNative.setPtyWindowSize(fd, rows, cols, cellWidth, cellHeight)
    }

    actual fun setPtyUTF8Mode(fd: Int) {
        TerminalNative.setPtyUTF8Mode(fd)
    }

    actual fun waitFor(pid: Int) = TerminalNative.waitFor(pid)

    actual fun close(fd: Int) {
        TerminalNative.close(fd)
    }

    actual fun readFromFd(fd: Int, buffer: ByteArray, maxLen: Int): Int {
        return TerminalNative.readFromFd(fd, buffer, maxLen)
    }

    actual fun writeToFd(fd: Int, buffer: ByteArray, len: Int): Int {
        return TerminalNative.writeToFd(fd, buffer, len)
    }

    actual fun readSymlink(path: String): String? {
        return TerminalNative.readSymlink(path)
    }

    actual fun killProcess(pid: Int, signal: Int): Int {
        return TerminalNative.killProcess(pid, signal)
    }
}
