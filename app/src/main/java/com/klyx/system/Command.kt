package com.klyx.system

import android.annotation.SuppressLint
import android.os.Environment
import com.klyx.data.fs.Paths
import com.klyx.terminal.home
import com.klyx.terminal.processEnv
import com.klyx.terminal.prootFile
import com.klyx.terminal.rootFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun command(program: String): CommandBuilder = CommandBuilder(program)

fun command(program: String, vararg args: Any): CommandBuilder =
    CommandBuilder(program).args(*args)

class CommandBuilder internal constructor(
    private val program: String,
) {
    private val args = mutableListOf<String>()
    private val env = mutableMapOf<String, String>()
    private var cwd: File? = null
    private var stdinSource: StdinSource = StdinSource.Inherit
    private var stdoutDest: StdioDest = StdioDest.Capture
    private var stderrDest: StdioDest = StdioDest.Capture

    fun arg(a: Any): CommandBuilder {
        args.add(a.toString())
        return this
    }

    fun args(vararg a: Any): CommandBuilder {
        a.forEach { args.add(it.toString()) }
        return this
    }

    fun args(a: List<Any>): CommandBuilder {
        a.forEach { args.add(it.toString()) }
        return this
    }

    fun env(key: String, value: String): CommandBuilder {
        env[key] = value
        return this
    }

    fun env(map: Map<String, String>): CommandBuilder {
        env.putAll(map)
        return this
    }

    fun cwd(dir: File): CommandBuilder {
        cwd = dir
        return this
    }

    fun cwd(path: String): CommandBuilder {
        cwd = File(path)
        return this
    }

    fun stdin(source: StdinSource): CommandBuilder {
        stdinSource = source
        return this
    }

    fun stdin(bytes: ByteArray): CommandBuilder {
        stdinSource = StdinSource.Bytes(bytes)
        return this
    }

    fun stdin(text: String): CommandBuilder {
        stdinSource = StdinSource.Bytes(text.encodeToByteArray())
        return this
    }

    fun stdout(dest: StdioDest): CommandBuilder {
        stdoutDest = dest
        return this
    }

    fun stderr(dest: StdioDest): CommandBuilder {
        stderrDest = dest
        return this
    }

    suspend fun output(): ProcessOutput {
        val child = spawnRaw()
        val stdinBytes = (stdinSource as? StdinSource.Bytes)?.data
        return withContext(Dispatchers.IO) {
            if (stdinBytes != null) {
                child.process.outputStream.use { it.write(stdinBytes) }
            }
            val outBytes = if (stdoutDest == StdioDest.Capture) {
                child.process.inputStream.readBytes()
            } else ByteArray(0)
            val errBytes = if (stderrDest == StdioDest.Capture) {
                child.process.errorStream.readBytes()
            } else ByteArray(0)
            child.process.waitFor()
            ProcessOutput(child.process.exitValue(), outBytes, errBytes)
        }
    }

    suspend fun spawn(): ChildProcess {
        val child = spawnRaw()
        val stdinBytes = (stdinSource as? StdinSource.Bytes)?.data
        if (stdinBytes != null) {
            withContext(Dispatchers.IO) {
                child.process.outputStream.use { it.write(stdinBytes) }
            }
        }
        return child
    }

    suspend fun stream(): Flow<ProcessOutputEvent> {
        stdoutDest = StdioDest.Capture
        stderrDest = StdioDest.Capture
        val child = spawnRaw()
        val stdinBytes = (stdinSource as? StdinSource.Bytes)?.data
        if (stdinBytes != null) {
            withContext(Dispatchers.IO) {
                child.process.outputStream.use { it.write(stdinBytes) }
            }
        }
        return child.flow()
    }

    suspend fun status(): Int {
        val child = spawnRaw()
        return withContext(Dispatchers.IO) {
            child.process.waitFor()
            child.process.exitValue()
        }
    }

    private fun spawnRaw(): ChildProcess {
        val resolved = resolveProgram(program)
        val pb = buildProcess(resolved)
        val process = pb.start()
        return ChildProcess(process)
    }

    @SuppressLint("SdCardPath")
    private fun buildProcess(resolved: ResolvedProgram): ProcessBuilder {
        val outDest = stdoutDest
        val errDest = stderrDest

        val pb = when (resolved) {
            is ResolvedProgram.Direct -> {
                ProcessBuilder(listOf(resolved.path) + args)
            }

            is ResolvedProgram.PRoot -> {
                val rootFsPath = Paths.rootFs.absolutePath
                val homePath = Paths.home.absolutePath
                val prootArgs = mutableListOf(
                    prootFile().absolutePath,
                    "-0",
                    "--kill-on-exit",
                    "--link2symlink",
                    "--sysvipc",
                    "-L",
                    "-r", rootFsPath,
                    "-w", "/root",
                    "-b", "/dev",
                    "-b", "/proc",
                    "-b", "/sys",
                    "-b", "/sdcard",
                    "-b", "/storage",
                    "-b", Paths.dataDir.canonicalPath, // /data/data/com.klyx
                    "-b", Paths.dataDir.absolutePath, // /data/user/0/com.klyx
                    "-b", "${homePath}:/root",
                    resolved.path,
                )
                prootArgs.addAll(args)
                val linker = "/system/bin/linker64"
                ProcessBuilder(listOf(linker) + prootArgs)
            }
        }

        val envMap = pb.environment()
        envMap.clear()
        try {
            envMap.putAll(processEnv())
        } catch (_: Throwable) {
            // Running outside Android (JVM tests, etc.)
            // processEnv() not available, proceed with just user env
            envMap.putAll(System.getenv())
        }
        envMap.putAll(env)

        cwd?.let { pb.directory(it) }

        when (stdinSource) {
            StdinSource.Inherit -> pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
            // Bytes and Pipe use the default PIPE mode. bytes are written
            // after process start in output() / spawn()
            is StdinSource.Bytes, StdinSource.Pipe -> {}
        }

        when (outDest) {
            StdioDest.Inherit -> pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
            StdioDest.Capture -> {}
            StdioDest.Null -> pb.redirectOutput(ProcessBuilder.Redirect.to(File("/dev/null")))
            is StdioDest.File -> pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outDest.file))
        }

        when (errDest) {
            StdioDest.Inherit -> pb.redirectError(ProcessBuilder.Redirect.INHERIT)
            StdioDest.Capture -> {}
            StdioDest.Null -> pb.redirectError(ProcessBuilder.Redirect.to(File("/dev/null")))
            is StdioDest.File -> pb.redirectError(ProcessBuilder.Redirect.appendTo(errDest.file))
        }

        return pb
    }
}

sealed interface StdinSource {
    data object Inherit : StdinSource
    data object Pipe : StdinSource

    data class Bytes(val data: ByteArray) : StdinSource {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Bytes

            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }
}

sealed interface StdioDest {
    data object Inherit : StdioDest
    data object Capture : StdioDest
    data object Null : StdioDest

    data class File(val file: java.io.File) : StdioDest
}

data class ProcessOutput(
    val exitCode: Int,
    val stdout: ByteArray,
    val stderr: ByteArray,
) {
    val stdoutText: String get() = stdout.decodeToString()
    val stderrText: String get() = stderr.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessOutput

        if (exitCode != other.exitCode) return false
        if (!stdout.contentEquals(other.stdout)) return false
        if (!stderr.contentEquals(other.stderr)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exitCode
        result = 31 * result + stdout.contentHashCode()
        result = 31 * result + stderr.contentHashCode()
        return result
    }
}

sealed interface ProcessOutputEvent {
    data class Stdout(val data: ByteArray) : ProcessOutputEvent {
        val text: String get() = data.decodeToString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Stdout
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
    }

    data class Stderr(val data: ByteArray) : ProcessOutputEvent {
        val text: String get() = data.decodeToString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Stderr
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
    }

    data class ExitCode(val code: Int) : ProcessOutputEvent
}

class ChildProcess internal constructor(
    internal val process: Process,
) {
    val pid: Int get() = process.pid()
    val isRunning: Boolean get() = process.isAlive
    val stdin: OutputStream get() = process.outputStream
    val stdout: InputStream get() = process.inputStream
    val stderr: InputStream get() = process.errorStream

    val exitCode: Int
        get() = if (process.isAlive) {
            throw IllegalStateException("Process is still running")
        } else {
            process.exitValue()
        }

    suspend fun waitFor(): ProcessOutput = withContext(Dispatchers.IO) {
        val outBytes = process.inputStream.readBytes()
        val errBytes = process.errorStream.readBytes()
        process.waitFor()
        ProcessOutput(process.exitValue(), outBytes, errBytes)
    }

    suspend fun waitForTimeout(timeoutMillis: Long): ProcessOutput? = withContext(Dispatchers.IO) {
        val exited = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!exited) return@withContext null
        val outBytes = if (process.inputStream.available() > 0) process.inputStream.readBytes() else ByteArray(0)
        val errBytes = if (process.errorStream.available() > 0) process.errorStream.readBytes() else ByteArray(0)
        ProcessOutput(process.exitValue(), outBytes, errBytes)
    }

    suspend fun waitForTimeout(timeout: Duration) = waitForTimeout(timeout.inWholeMilliseconds)

    fun flow(): Flow<ProcessOutputEvent> = channelFlow {
        val bufferSize = 8192

        val outJob = launch(Dispatchers.IO) {
            try {
                val buffer = ByteArray(bufferSize)
                var bytesRead: Int
                while (process.inputStream.read(buffer).also { bytesRead = it } >= 0) {
                    if (bytesRead > 0) {
                        send(ProcessOutputEvent.Stdout(buffer.copyOf(bytesRead)))
                    }
                }
            } catch (_: IOException) {}
        }

        val errJob = launch(Dispatchers.IO) {
            try {
                val buffer = ByteArray(bufferSize)
                var bytesRead: Int
                while (process.errorStream.read(buffer).also { bytesRead = it } >= 0) {
                    if (bytesRead > 0) {
                        send(ProcessOutputEvent.Stderr(buffer.copyOf(bytesRead)))
                    }
                }
            } catch (_: IOException) {}
        }

        try {
            withContext(Dispatchers.IO) {
                process.waitFor()
            }
            outJob.join()
            errJob.join()
            send(ProcessOutputEvent.ExitCode(process.exitValue()))
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
            }
        }
    }

    fun kill(): ChildProcess = ChildProcess(process.destroyForcibly())
    fun terminate() = process.destroy()
}

sealed interface ResolvedProgram {
    data class Direct(val path: String) : ResolvedProgram
    data class PRoot(val path: String) : ResolvedProgram
}

internal val ROOTFS_BIN_PATHS = listOf(
    "/usr/local/bin", "/usr/bin", "/bin",
    "/usr/local/sbin", "/usr/sbin", "/sbin",
)

internal val SYSTEM_BIN_PATHS = listOf(
    "/system/bin", "/system/xbin", "/vendor/bin",
)

internal fun resolveProgram(program: String): ResolvedProgram {
    val rootFs = try {
        Paths.rootFs.takeIf { it.exists() }
    } catch (_: Exception) {
        null
    }

    if (program.contains(File.separatorChar)) {
        if (rootFs != null) {
            val inRootfs = rootFs.resolve(program.trimStart('/'))
            if (inRootfs.exists()) {
                return ResolvedProgram.PRoot(inRootfs.absolutePath)
            }
        }
        return ResolvedProgram.Direct(program)
    }

    if (rootFs != null) {
        for (dir in ROOTFS_BIN_PATHS) {
            val f = rootFs.resolve(dir.trimStart('/')).resolve(program)
            if (f.exists()) {
                return ResolvedProgram.PRoot(f.absolutePath)
            }
        }
    }

    for (dir in SYSTEM_BIN_PATHS) {
        val f = File(dir, program)
        if (f.exists()) {
            return ResolvedProgram.Direct(f.absolutePath)
        }
    }

    return ResolvedProgram.Direct(program)
}
