@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm.wasi

import com.klyx.core.logging.logger
import com.klyx.nullInputStream
import com.klyx.nullOutputStream
import com.klyx.systemNanoTime
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostFunctionCase
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.utils.toBytesLE
import com.klyx.wasm.wasi.Descriptors.Directory
import com.klyx.wasm.wasi.Descriptors.InStream
import com.klyx.wasm.wasi.Descriptors.OpenFile
import com.klyx.wasm.wasi.Descriptors.OutStream
import com.klyx.wasm.wasi.Descriptors.PreopenedDirectory
import kotlinx.datetime.Clock
import kotlinx.io.IOException
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random

/**
 * [WASI preview 1](https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md) implementation
 */
@HostModule("wasi_snapshot_preview1")
class WasiP1(
    private val random: Random = Random(Random.nextLong()),
    private val clock: Clock = Clock.System,
    private val stdout: OutputStream = nullOutputStream(),
    private val stderr: OutputStream = nullOutputStream(),
    private val stdin: InputStream = nullInputStream(),
    arguments: List<String> = emptyList(),
    environment: Map<String, String> = emptyMap(),
    private val directories: Map<String, Path> = emptyMap(),
    private val fileSystem: FileSystem = SystemFileSystem
) : AutoCloseable {
    private val logger = logger("WasiPreview1")
    private val descriptors = Descriptors()

    private val args = arguments.map { it.toBytesLE() }
    private val envp = environment.map { it.key.toBytesLE() to it.value.toBytesLE() }

    init {
        descriptors.apply {
            allocate(InStream(stdin.asSource().buffered()))
            allocate(OutStream(stdout.asSink().buffered()))
            allocate(OutStream(stderr.asSink().buffered()))

            for ((name, path) in directories) {
                val nameBytes = name.toBytesLE()
                allocate(PreopenedDirectory(nameBytes, path))
            }
        }
    }

    override fun close() {
        descriptors.closeAll()
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun adapterCloseBadfd(fd: Int): Int {
        logger.v("adapter_close_badfd: [$fd]")
        wasiCallNotSupported("adapter_close_badfd")
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun adapterOpenBadfd(fd: Int): Int {
        logger.v("adapter_open_badfd: [$fd]")
        wasiCallNotSupported("adapter_open_badfd")
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun argsGet(memory: WasmMemory, argv: Int, argvBuf: Int): Int {
        logger.v("args_get: [$argv, $argvBuf]")

        var argvOffset = argv
        var argvBufOffset = argvOffset

        for (arg in args) {
            memory.writeI32(argvOffset, argvBuf)
            argvOffset += 4
            memory.write(argvBufOffset, arg)
            argvBufOffset += arg.size
            memory.writeByte(argvBufOffset, 0.toByte())
            argvBufOffset++
        }

        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun argsSizesGet(memory: WasmMemory, argc: Int, argvBufSize: Int): Int {
        logger.v("args_sizes_get: [$argc, $argvBufSize]")
        val bufSize = args.sumOf { it.size + 1 }
        memory.writeI32(argc, args.size)
        memory.writeI32(argvBufSize, bufSize)
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun clockResGet(memory: WasmMemory, clockId: Int, resultPtr: Int): Int {
        logger.v("clock_res_get: [$clockId, $resultPtr]")
        return when (clockId) {
            WasiClockId.Realtime, WasiClockId.Monotonic -> {
                memory.writeLong(resultPtr, 1L)
                wasiResult(WasiErrno.ESUCCESS)
            }

            WasiClockId.ThreadCputimeId, WasiClockId.ProcessCputimeId -> {
                wasiResult(WasiErrno.ENOTSUP)
            }

            else -> wasiResult(WasiErrno.EINVAL)
        }
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun clockTimeGet(memory: WasmMemory, clockId: Int, precision: Long, resultPtr: Int): Int {
        logger.v("clock_time_get: [$clockId, $precision, $resultPtr]")
        return when (clockId) {
            WasiClockId.Monotonic, WasiClockId.Realtime -> {
                memory.writeLong(resultPtr, clockTime(clockId))
                wasiResult(WasiErrno.ESUCCESS)
            }

            WasiClockId.ProcessCputimeId, WasiClockId.ThreadCputimeId -> wasiResult(WasiErrno.ENOTSUP)
            else -> wasiResult(WasiErrno.EINVAL)
        }
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun environGet(memory: WasmMemory, environ: Int, environBuf: Int): Int {
        logger.v("environ_get: [$environ, $environBuf]")

        var environOffset = environ
        var environBufOffset = environBuf

        for ((name, value) in envp) {
            val data = ByteArray(name.size + value.size + 2)

            name.copyInto(data, 0, 0, name.size)
            data[name.size] = '='.code.toByte()
            value.copyInto(data, name.size + 1, 0, value.size)
            data[data.lastIndex] = 0

            memory.writeI32(environOffset, environBufOffset)
            environOffset += 4
            memory.write(environBufOffset, data)
            environBufOffset += data.size
        }
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun environSizesGet(memory: WasmMemory, environCount: Int, environBufSize: Int): Int {
        logger.v { "environ_sizes_get: [$environCount, $environBufSize]" }
        val bufSize = envp.sumOf { (key, value) -> key.size + value.size + 2 }
        memory.writeI32(environCount, envp.size)
        memory.writeI32(environBufSize, bufSize)
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdAdvise(fd: Int, offset: Long, len: Long, advice: Int): Int {
        logger.v("fd_advise: [$fd, $offset, $len, $advice]")

        if (len < 0 || offset < 0) {
            return wasiResult(WasiErrno.EINVAL)
        }

        val descriptor = descriptors[fd] ?: return wasiResult(WasiErrno.EBADF)

        when (descriptor) {
            is InStream, is OutStream -> return wasiResult(WasiErrno.ESPIPE)
            is Directory -> return wasiResult(WasiErrno.EISDIR)
            !is OpenFile -> unhandledDescriptor(descriptor)
        }

        // do nothing: advise is optional
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdAllocate(fd: Int, offset: Long, len: Long): Int {
        logger.v("fd_allocate: [$fd, $offset, $len]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdClose(fd: Int): Int {
        logger.v("fd_close: [$fd]")
        val descriptor = descriptors[fd] ?: return wasiResult(WasiErrno.EBADF)
        descriptors.free(fd)

        try {
            if (descriptor is AutoCloseable) {
                descriptor.close()
            }
        } catch (e: IOException) {
            return wasiResult(WasiErrno.EIO)
        }

        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdDatasync(fd: Int): Int {
        logger.v("fd_datasync: [$fd]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdFdstatGet(memory: WasmMemory, fd: Int, buf: Int): Int {
        logger.v("fd_fdstat_get: [$fd, $buf]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdFdstatSetFlags(fd: Int, flags: Int): Int {
        logger.v("fd_fdstat_set_flags: [$fd, $flags]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdFdstatSetRights(fd: Int, rightsBase: Long, rightsInheriting: Long): Int {
        logger.v("fd_fdstat_set_rights: [$fd, $rightsBase, $rightsInheriting]")
        wasiCallNotSupported("fd_fdstat_set_rights")
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdFilestatGet(memory: WasmMemory, fd: Int, buf: Int): Int {
        logger.v("fd_filestat_get: [$fd, $buf]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdFilestatSetSize(fd: Int, size: Long): Int {
        logger.v("fd_filestat_set_size: [$fd, $size]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdFilestatSetTimes(fd: Int, accessTime: Long, modifiedTime: Long, fstFlags: Int): Int {
        logger.v("fd_filestat_set_times: [$fd, $accessTime, $modifiedTime, $fstFlags]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdPread(
        memory: WasmMemory,
        fd: Int,
        iovs: Int,
        iovsLen: Int,
        offset: Long,
        nreadPtr: Int
    ): Int {
        logger.v("fd_pread: [$fd, $iovs, $iovsLen, $offset, $nreadPtr]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdPrestatDirName(memory: WasmMemory, fd: Int, path: Int, pathLen: Int): Int {
        logger.v("fd_prestat_dir_name: [$fd, $path, $pathLen]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdPrestatGet(memory: WasmMemory, fd: Int, buf: Int): Int {
        logger.v("fd_prestat_get: [$fd, $buf]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdPwrite(
        memory: WasmMemory,
        fd: Int,
        iovs: Int,
        iovsLen: Int,
        offset: Long,
        nwrittenPtr: Int
    ): Int {
        logger.v("fd_pwrite: [$fd, $iovs, $iovsLen, $offset, $nwrittenPtr]")

        if (offset < 0) {
            return wasiResult(WasiErrno.EINVAL)
        }
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdRead(memory: WasmMemory, fd: Int, iovs: Int, iovsLen: Int, nreadPtr: Int): Int {
        logger.v("fd_read: [$fd, $iovs, $iovsLen, $nreadPtr]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdReaddir(
        memory: WasmMemory,
        dirFd: Int,
        buf: Int,
        bufLen: Int,
        cookie: Long,
        bufUsedPtr: Int
    ): Int {
        logger.v("fd_readdir: [$dirFd, $buf, $bufLen, $cookie, $bufUsedPtr]")

        if (cookie < 0) {
            return wasiResult(WasiErrno.EINVAL)
        }
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdRenumber(from: Int, to: Int): Int {
        logger.v("fd_renumber: [$from, $to]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdSeek(memory: WasmMemory, fd: Int, offset: Long, whence: Int, newOffsetPtr: Int): Int {
        logger.v("fd_seek: [$fd, $offset, $whence, $newOffsetPtr]")

        if (whence < 0 || whence > 2) {
            return wasiResult(WasiErrno.EINVAL)
        }
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdSync(fd: Int): Int {
        logger.v("fd_sync: [$fd]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdTell(memory: WasmMemory, fd: Int, offsetPtr: Int): Int {
        logger.v("fd_tell: [$fd, $offsetPtr]")

        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun fdWrite(memory: WasmMemory, fd: Int, iovs: Int, iovsLen: Int, nwrittenPtr: Int): Int {
        logger.v("fd_write: [$fd, $iovs, $iovsLen, $nwrittenPtr]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathCreateDirectory(dirFd: Int, rawPath: String): Int {
        logger.v("path_create_directory: [$dirFd, \"$rawPath\"]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathFilestatGet(
        memory: WasmMemory,
        dirFd: Int,
        lookupFlags: Int,
        rawPath: String,
        buf: Int
    ): Int {
        logger.v("path_filestat_get: [$dirFd, $lookupFlags, \"$rawPath\", $buf]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathFilestatSetTimes(
        fd: Int,
        lookupFlags: Int,
        rawPath: String,
        accessTime: Long,
        modifiedTime: Long,
        fstFlags: Int
    ): Int {
        logger.v("path_filestat_set_times: [$fd, $lookupFlags, \"$rawPath\", $accessTime, $modifiedTime, $fstFlags]")
        // kotlinx.io doesn't support setting file times
        return wasiResult(WasiErrno.ENOTSUP)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathLink(
        oldFd: Int,
        oldFlags: Int,
        rawOldPath: String,
        newFd: Int,
        rawNewPath: String
    ): Int {
        logger.v("path_link: [$oldFd, $oldFlags, \"$rawOldPath\", $newFd, \"$rawNewPath\"]")
        // kotlinx.io doesn't support hard links
        return wasiResult(WasiErrno.ENOTSUP)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathOpen(
        memory: WasmMemory,
        dirFd: Int,
        lookupFlags: Int,
        rawPath: String,
        openFlags: Int,
        rightsBase: Long,
        rightsInheriting: Long,
        fdFlags: Int,
        fdPtr: Int
    ): Int {
        logger.v("path_open: [$dirFd, $lookupFlags, \"$rawPath\", $openFlags, $rightsBase, $rightsInheriting, $fdFlags, $fdPtr]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathReadlink(
        memory: WasmMemory,
        dirFd: Int,
        rawPath: String,
        buf: Int,
        bufLen: Int,
        bufUsedPtr: Int
    ): Int {
        logger.v("path_readlink: [$dirFd, \"$rawPath\", $buf, $bufLen, $bufUsedPtr]")
        // kotlinx.io doesn't support symbolic links
        return wasiResult(WasiErrno.ENOTSUP)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathRemoveDirectory(dirFd: Int, rawPath: String): Int {
        logger.v("path_remove_directory: [$dirFd, \"$rawPath\"]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathRename(oldFd: Int, oldRawPath: String, newFd: Int, newRawPath: String): Int {
        logger.v("path_rename: [$oldFd, \"$oldRawPath\", $newFd, \"$newRawPath\"]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathSymlink(oldRawPath: String, dirFd: Int, newRawPath: String): Int {
        logger.v("path_symlink: [\"$oldRawPath\", $dirFd, \"$newRawPath\"]")
        // kotlinx.io doesn't support symbolic links
        return wasiResult(WasiErrno.ENOTSUP)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pathUnlinkFile(dirFd: Int, rawPath: String): Int {
        logger.v("path_unlink_file: [$dirFd, \"$rawPath\"]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun pollOneoff(
        memory: WasmMemory,
        inPtr: Int,
        outPtr: Int,
        nsubscriptions: Int,
        neventsPtr: Int
    ): Int {
        logger.v("poll_oneoff: [$inPtr, $outPtr, $nsubscriptions, $neventsPtr]")
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun procExit(code: Int): Nothing {
        logger.v("proc_exit: [$code]")
        throw WasiExitException(code)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun procRaise(sig: Int): Int {
        logger.v("proc_raise: [$sig]")
        wasiCallNotSupported("proc_raise")
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun schedYield(): Int {
        logger.v("sched_yield")
        // do nothing here
        return wasiResult(WasiErrno.ESUCCESS)
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun sockAccept(sock: Int, fdFlags: Int, roFdPtr: Int): Int {
        logger.v("sock_accept: [$sock, $fdFlags, $roFdPtr]")
        wasiCallNotSupported("sock_accept")
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun sockRecv(
        sock: Int,
        riDataPtr: Int,
        riDataLen: Int,
        riFlags: Int,
        roDataLenPtr: Int,
        roFlagsPtr: Int
    ): Int {
        logger.v("sock_recv: [$sock, $riDataPtr, $riDataLen, $riFlags, $roDataLenPtr, $roFlagsPtr]")
        wasiCallNotSupported("sock_recv")
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun sockSend(sock: Int, siDataPtr: Int, siDataLen: Int, siFlags: Int, retDataLenPtr: Int): Int {
        logger.v("sock_send: [$sock, $siDataPtr, $siDataLen, $siFlags, $retDataLenPtr]")
        wasiCallNotSupported("sock_send")
    }

    @HostFunction(case = HostFunctionCase.SnakeCase)
    fun sockShutdown(sock: Int, how: Int): Int {
        logger.v("sock_shutdown: [$sock, $how]")
        val descriptor = descriptors[sock] ?: return wasiResult(WasiErrno.EBADF)
        // sockets are not supported, so this cannot be a socket
        return wasiResult(WasiErrno.ENOTSOCK)
    }

    private fun wasiResult(errno: WasiErrno): Int {
        if (errno != WasiErrno.ESUCCESS) {
            logger.v("result = ${errno.name}")
        }
        return errno.value
    }

    private fun clockTime(clockId: Int) = when (clockId) {
        WasiClockId.Realtime -> {
            val now = clock.now()
            now.epochSeconds * 1_000_000_000L + now.nanosecondsOfSecond
        }

        WasiClockId.Monotonic -> systemNanoTime()

        else -> error("Invalid clockId: $clockId")
    }

    private fun unhandledDescriptor(descriptor: Descriptors.Descriptor): Nothing {
        throw WasiRuntimeException("Unhandled descriptor: " + descriptor::class.simpleName)
    }
}
