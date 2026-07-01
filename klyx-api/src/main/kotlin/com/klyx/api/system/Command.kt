package com.klyx.api.system

import android.annotation.SuppressLint
import com.klyx.api.data.fs.Paths
import com.klyx.api.terminal.home
import com.klyx.api.terminal.processEnv
import com.klyx.api.terminal.prootFile
import com.klyx.api.terminal.rootFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Creates a [CommandBuilder] to run the specified [program].
 *
 * The program will be searched for in the terminal rootfs first, then in the system paths.
 */
fun command(program: String): CommandBuilder = CommandBuilder(program)

/**
 * Creates a [CommandBuilder] to run the specified [program] with the given [args].
 */
fun command(program: String, vararg args: Any): CommandBuilder =
    CommandBuilder(program).args(*args)

/**
 * A builder for configuring and executing system processes.
 *
 * This class provides a DSL-like API for setting up arguments, environment variables,
 * working directories, and I/O redirection for a command.
 *
 * It automatically handles running binaries within the Klyx PRoot environment if they
 * are found in the terminal rootfs.
 */
class CommandBuilder internal constructor(
    private val program: String,
) {
    private val args = mutableListOf<String>()
    private val env = mutableMapOf<String, String>()
    private var cwd: File? = null
    private var stdinSource: StdinSource = StdinSource.Inherit
    private var stdoutDest: StdioDest = StdioDest.Capture
    private var stderrDest: StdioDest = StdioDest.Capture

    /** Adds a single argument to the command. */
    fun arg(a: Any): CommandBuilder {
        args.add(a.toString())
        return this
    }

    /** Adds multiple arguments to the command. */
    fun args(vararg a: Any): CommandBuilder {
        a.forEach { args.add(it.toString()) }
        return this
    }

    /** Adds a list of arguments to the command. */
    fun args(a: List<Any>): CommandBuilder {
        a.forEach { args.add(it.toString()) }
        return this
    }

    /** Adds an environment variable. */
    fun env(key: String, value: String): CommandBuilder {
        env[key] = value
        return this
    }

    /** Adds multiple environment variables from a map. */
    fun env(map: Map<String, String>): CommandBuilder {
        env.putAll(map)
        return this
    }

    /** Sets the current working directory for the process. */
    fun cwd(dir: File): CommandBuilder {
        cwd = dir
        return this
    }

    /** Sets the current working directory path for the process. */
    fun cwd(path: String): CommandBuilder {
        cwd = File(path)
        return this
    }

    /** Configures where the process reads its standard input from. */
    fun stdin(source: StdinSource): CommandBuilder {
        stdinSource = source
        return this
    }

    /** Provides standard input to the process as a [ByteArray]. */
    fun stdin(bytes: ByteArray): CommandBuilder {
        stdinSource = StdinSource.Bytes(bytes)
        return this
    }

    /** Provides standard input to the process as a [String]. */
    fun stdin(text: String): CommandBuilder {
        stdinSource = StdinSource.Bytes(text.encodeToByteArray())
        return this
    }

    /** Configures where the process writes its standard output. */
    fun stdout(dest: StdioDest): CommandBuilder {
        stdoutDest = dest
        return this
    }

    /** Configures where the process writes its standard error. */
    fun stderr(dest: StdioDest): CommandBuilder {
        stderrDest = dest
        return this
    }

    /**
     * Executes the command and waits for it to finish, capturing all output.
     *
     * @return A [ProcessOutput] containing the exit code and captured stdout/stderr.
     */
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

    /**
     * Starts the process and returns a [ChildProcess] handle.
     */
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

    /**
     * Executes the command and returns a [Flow] of [ProcessOutputEvent]s.
     * Standard output and error are automatically set to [StdioDest.Capture].
     */
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

    /**
     * Executes the command and waits for it to finish, returning only the exit code.
     */
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

/**
 * Defines the source of the process's standard input.
 */
sealed interface StdinSource {

    /** The process inherits the parent process's standard input. */
    data object Inherit : StdinSource

    /** Standard input is provided via a pipe that can be written to. */
    data object Pipe : StdinSource

    /** Standard input is provided as a fixed [ByteArray]. */
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

/**
 * Defines where the process's standard output or error should be directed.
 */
sealed interface StdioDest {

    /** Output is inherited from the parent process. */
    data object Inherit : StdioDest

    /** Output is captured and can be read as bytes or text. */
    data object Capture : StdioDest

    /** Output is discarded (sent to `/dev/null`). */
    data object Null : StdioDest

    /** Output is appended to the specified [file]. */
    data class File(val file: java.io.File) : StdioDest
}

/**
 * Captured results of a process execution.
 *
 * @property exitCode The process exit code (typically 0 for success).
 * @property stdout Captured standard output as bytes.
 * @property stderr Captured standard error as bytes.
 */
data class ProcessOutput(
    val exitCode: Int,
    val stdout: ByteArray,
    val stderr: ByteArray,
) {
    /** The captured standard output decoded as a UTF-8 string. */
    val stdoutText: String get() = stdout.decodeToString()

    /** The captured standard error decoded as a UTF-8 string. */
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

/**
 * Events emitted during a streaming process execution.
 */
sealed interface ProcessOutputEvent {

    /** Standard output data chunk. */
    data class Stdout(val data: ByteArray) : ProcessOutputEvent {
        /** The data chunk decoded as a UTF-8 string. */
        val text: String get() = data.decodeToString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Stdout
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
    }

    /** Standard error data chunk. */
    data class Stderr(val data: ByteArray) : ProcessOutputEvent {
        /** The data chunk decoded as a UTF-8 string. */
        val text: String get() = data.decodeToString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Stderr
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
    }

    /** The process has finished with the given [code]. */
    data class ExitCode(val code: Int) : ProcessOutputEvent
}

/**
 * A handle to a running process, providing methods to wait for its completion or terminate it.
 */
class ChildProcess internal constructor(
    internal val process: Process,
) {
    /** The Process ID (PID) of the child process. */
    val pid: Int get() = process.pid()

    /** Whether the process is still running. */
    val isRunning: Boolean get() = process.isAlive

    /** The stream used to write to the process's standard input. */
    val stdin: OutputStream get() = process.outputStream

    /** The stream used to read from the process's standard output. */
    val stdout: InputStream get() = process.inputStream

    /** The stream used to read from the process's standard error. */
    val stderr: InputStream get() = process.errorStream

    /** The process exit code. Throws [IllegalStateException] if the process is still running. */
    val exitCode: Int
        get() = if (process.isAlive) {
            throw IllegalStateException("Process is still running")
        } else {
            process.exitValue()
        }

    /**
     * Suspends until the process completes and returns its full output.
     */
    suspend fun waitFor(): ProcessOutput = withContext(Dispatchers.IO) {
        val outBytes = process.inputStream.readBytes()
        val errBytes = process.errorStream.readBytes()
        process.waitFor()
        ProcessOutput(process.exitValue(), outBytes, errBytes)
    }

    /**
     * Suspends until the process completes or the [timeoutMillis] is reached.
     *
     * @return The [ProcessOutput] if the process finished, or null if it timed out.
     */
    suspend fun waitForTimeout(timeoutMillis: Long): ProcessOutput? = withContext(Dispatchers.IO) {
        val exited = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!exited) return@withContext null
        val outBytes = if (process.inputStream.available() > 0) process.inputStream.readBytes() else ByteArray(0)
        val errBytes = if (process.errorStream.available() > 0) process.errorStream.readBytes() else ByteArray(0)
        ProcessOutput(process.exitValue(), outBytes, errBytes)
    }

    /**
     * Suspends until the process completes or the [timeout] is reached.
     */
    suspend fun waitForTimeout(timeout: Duration) = waitForTimeout(timeout.inWholeMilliseconds)

    /**
     * Returns a [Flow] that emits stdout/stderr chunks and the final exit code as they occur.
     * The process is automatically forcibly destroyed if the flow collection is cancelled.
     */
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
            } catch (_: IOException) {
            }
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
            } catch (_: IOException) {
            }
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

    /** Forcibly kills the child process. */
    fun kill(): ChildProcess = ChildProcess(process.destroyForcibly())

    /** Gracefully terminates the child process. */
    fun terminate() = process.destroy()
}

/**
 * Represents a program that has been resolved to a specific execution path.
 */
sealed interface ResolvedProgram {

    /** A binary that can be executed directly by the system. */
    data class Direct(val path: String) : ResolvedProgram

    /** A binary that must be executed within the PRoot environment. */
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
