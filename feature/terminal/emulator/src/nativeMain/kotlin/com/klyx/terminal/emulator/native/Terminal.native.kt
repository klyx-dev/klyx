package com.klyx.terminal.emulator.native

import com.klyx.terminal.c.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
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
        return memScoped {
            val cArgs = allocArray<CPointerVar<ByteVar>>(args.size + 1)
            for (i in args.indices) {
                cArgs[i] = args[i].cstr.ptr
            }
            cArgs[args.size] = null

            val cEnvVars = allocArray<CPointerVar<ByteVar>>(envVars.size + 1)
            for (i in envVars.indices) {
                cEnvVars[i] = envVars[i].cstr.ptr
            }
            cEnvVars[envVars.size] = null

            val pProcessId = alloc<IntVar>()
            val errorMessage = alloc<CPointerVar<ByteVar>>()

            val ptm = term_create_subprocess(
                cmd.cstr.ptr,
                cwd.cstr.ptr,
                cArgs,
                cEnvVars,
                pProcessId.ptr,
                rows,
                columns,
                cellWidth,
                cellHeight,
                errorMessage.ptr
            )

            if (ptm < 0) {
                val errorStr = errorMessage.value?.toKString() ?: "Unknown terminal spawn error"
                throw RuntimeException(errorStr)
            }

            processIdArray[0] = pProcessId.value
            ptm
        }
    }

    actual fun setPtyWindowSize(fd: Int, rows: Int, cols: Int, cellWidth: Int, cellHeight: Int) {
        term_set_pty_window_size(fd, rows, cols, cellWidth, cellHeight)
    }

    actual fun setPtyUTF8Mode(fd: Int) {
        term_set_pty_utf8_mode(fd)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun waitFor(pid: Int): Int {
        return term_wait_for(pid)
    }

    actual fun close(fd: Int) {
        term_close(fd)
    }

    actual fun readFromFd(fd: Int, buffer: ByteArray, maxLen: Int): Int {
        return buffer.usePinned { pinned ->
            term_read_from_fd(fd, pinned.addressOf(0), maxLen.toULong()).toInt()
        }
    }

    actual fun writeToFd(fd: Int, buffer: ByteArray, len: Int): Int {
        return buffer.usePinned { pinned ->
            term_write_to_fd(fd, pinned.addressOf(0), len.toULong()).toInt()
        }
    }

    actual fun readSymlink(path: String): String? {
        return memScoped {
            val buffer = allocArray<ByteVar>(1024)
            val n = term_read_symlink(path, buffer, 1024u)

            if (n < 0L) null else buffer.toKString()
        }
    }

    actual fun killProcess(pid: Int, signal: Int): Int {
        return term_kill_process(pid, signal)
    }
}
