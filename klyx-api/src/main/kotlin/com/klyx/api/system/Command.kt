@file:Suppress("unused")

package com.klyx.api.system

import android.annotation.SuppressLint
import com.klyx.api.data.fs.Paths
import com.klyx.api.terminal.home
import com.klyx.api.terminal.processEnv
import com.klyx.api.terminal.prootFile
import com.klyx.api.terminal.rootFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

internal val EMPTY_BYTE_ARRAY = ByteArray(0)

/**
 * Creates a [Command] to run the specified [program].
 *
 * The program will be searched for in the terminal rootfs first, then in the system paths.
 */
fun command(program: String): Command = Command(program)

/**
 * Creates a [Command] to run the specified [program] with the given [args].
 */
fun command(program: String, vararg args: Any): Command =
    Command(program).args(*args)

/**
 * A builder for configuring and executing system processes.
 *
 * This class provides a DSL-like API for setting up arguments, environment variables,
 * working directories, and I/O redirection for a command.
 *
 * It automatically handles running binaries within the Klyx PRoot environment if they
 * are found in the terminal rootfs.
 */
class Command internal constructor(
    internal val program: String,
) {
    internal val args = mutableListOf<String>()
    internal val env = mutableMapOf<String, String>()
    internal var cwd: File? = null
    internal var stdinSource: Stdin = Stdin.Pipe
    internal var stdoutDest: Stdio = Stdio.Capture
    internal var stderrDest: Stdio = Stdio.Capture

    internal data class IoConfig(
        val stdin: Stdin,
        val stdout: Stdio,
        val stderr: Stdio,
    )

    private fun ioConfig(): IoConfig = IoConfig(stdinSource, stdoutDest, stderrDest)

    /** Adds a single argument to the command. */
    fun arg(a: Any): Command {
        args.add(a.toString())
        return this
    }

    /** Adds multiple arguments to the command. */
    fun args(vararg a: Any): Command {
        a.forEach { args.add(it.toString()) }
        return this
    }

    /** Adds a list of arguments to the command. */
    fun args(a: List<Any>): Command {
        a.forEach { args.add(it.toString()) }
        return this
    }

    /** Adds an environment variable. */
    fun env(key: String, value: String): Command {
        env[key] = value
        return this
    }

    /** Adds multiple environment variables from a map. */
    fun env(map: Map<String, String>): Command {
        env.putAll(map)
        return this
    }

    /** Sets the current working directory for the process. */
    fun cwd(dir: File): Command {
        cwd = dir
        return this
    }

    /** Sets the current working directory path for the process. */
    fun cwd(path: String): Command {
        cwd = File(path)
        return this
    }

    /** Configures where the process reads its standard input from. */
    fun stdin(source: Stdin): Command {
        stdinSource = source
        return this
    }

    /** Provides standard input to the process as a [ByteArray]. */
    fun stdin(bytes: ByteArray): Command {
        stdinSource = Stdin.Bytes(bytes)
        return this
    }

    /** Provides standard input to the process as a [String]. */
    fun stdin(text: String): Command {
        stdinSource = Stdin.Bytes(text.encodeToByteArray())
        return this
    }

    /** Configures where the process writes its standard output. */
    fun stdout(dest: Stdio): Command {
        stdoutDest = dest
        return this
    }

    /** Configures where the process writes its standard error. */
    fun stderr(dest: Stdio): Command {
        stderrDest = dest
        return this
    }

    /**
     * Executes the command and waits for it to finish, capturing all output.
     *
     * @return A [CommandResult] containing the exit code and captured stdout/stderr.
     */
    suspend fun output(): CommandResult = coroutineScope {
        val config = ioConfig()
        val child = spawnRaw(config)
        val stdinBytes = (config.stdin as? Stdin.Bytes)?.data
        withContext(Dispatchers.IO) {
            if (stdinBytes != null) child.process.outputStream.use { it.write(stdinBytes) }
        }
        val outDeferred = if (config.stdout == Stdio.Capture) {
            async(Dispatchers.IO) { child.process.inputStream.readBytes() }
        } else null
        val errDeferred = if (config.stderr == Stdio.Capture) {
            async(Dispatchers.IO) { child.process.errorStream.readBytes() }
        } else null
        withContext(Dispatchers.IO) { child.process.waitFor() }
        CommandResult(
            exitCode = child.process.exitValue(),
            stdout = outDeferred?.await() ?: EMPTY_BYTE_ARRAY,
            stderr = errDeferred?.await() ?: EMPTY_BYTE_ARRAY,
        )
    }

    /**
     * Starts the process and returns a [ProcessHandle] handle.
     */
    suspend fun spawn(): ProcessHandle {
        val child = spawnRaw()
        val stdinBytes = (stdinSource as? Stdin.Bytes)?.data
        if (stdinBytes != null) {
            withContext(Dispatchers.IO) {
                child.process.outputStream.write(stdinBytes)
            }
        }
        return child
    }

    /**
     * Executes the command and returns a [Flow] of [ProcessEvent]s.
     * Standard output and error are automatically set to [Stdio.Capture].
     */
    suspend fun stream(): Flow<ProcessEvent> {
        val config = IoConfig(stdinSource, Stdio.Capture, Stdio.Capture)
        val child = spawnRaw(config)
        val stdinBytes = (config.stdin as? Stdin.Bytes)?.data
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
        val config = IoConfig(stdinSource, Stdio.Null, Stdio.Null)
        val child = spawnRaw(config)
        return withContext(Dispatchers.IO) {
            child.process.waitFor()
            child.process.exitValue()
        }
    }

    internal suspend fun spawnRaw(config: IoConfig = ioConfig()): ProcessHandle {
        val process = createProcess(
            program = program,
            args = args,
            env = env,
            cwd = cwd,
            stdin = config.stdin,
            stdout = config.stdout,
            stderr = config.stderr,
        )
        return ProcessHandle(process)
    }

    companion object {
        /** Creates a [Command] that executes [script] via `sh -c`. */
        fun shell(script: String): Command = Command("sh").args("-c", script)
    }
}

@Deprecated("Use Command", ReplaceWith("Command"))
typealias CommandBuilder = Command

internal suspend fun createProcess(
    program: String,
    args: List<String>,
    env: Map<String, String>,
    cwd: File?,
    stdin: Stdin,
    stdout: Stdio,
    stderr: Stdio,
): Process {
    val resolved = resolveProgram(program)
    val pb = buildProcessBuilder(resolved, args, env, cwd, stdin, stdout, stderr)
    return withContext(Dispatchers.IO) { pb.start() }
}

@SuppressLint("SdCardPath")
internal fun buildProcessBuilder(
    resolved: ResolvedProgram,
    args: List<String>,
    env: Map<String, String>,
    cwd: File?,
    stdin: Stdin,
    stdout: Stdio,
    stderr: Stdio,
): ProcessBuilder {
    val pb = when (resolved) {
        is ResolvedProgram.Direct -> ProcessBuilder(listOf(resolved.path) + args)
        is ResolvedProgram.PRoot -> {
            val rootFsPath = Paths.rootFs.absolutePath
            val homePath = Paths.home.absolutePath
            val guestCmd = buildString {
                append(resolved.guestPath)
                for (arg in args) {
                    append(' ')
                    append(shellEscape(arg))
                }
            }
            val prootArgs = mutableListOf(
                prootFile().absolutePath,
                "-0", "--kill-on-exit", "--link2symlink", "--sysvipc", "-L",
                "-r", rootFsPath,
                "-w", "/root",
                "-b", "/dev", "-b", "/proc", "-b", "/sys",
                "-b", "/sdcard", "-b", "/storage",
                "-b", Paths.dataDir.canonicalPath,
                "-b", Paths.dataDir.absolutePath,
                "-b", "${homePath}:/root",
                "/bin/bash", "-lc", guestCmd,
            )
            ProcessBuilder(prootArgs)
        }
    }

    val envMap = pb.environment()
    envMap.clear()
    try {
        envMap.putAll(processEnv())
    } catch (_: Throwable) {
        envMap.putAll(System.getenv())
    }
    envMap.putAll(env)

    cwd?.let { pb.directory(it) }

    when (stdin) {
        Stdin.Inherit -> pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
        is Stdin.Bytes, Stdin.Pipe -> pb.redirectInput(ProcessBuilder.Redirect.PIPE)
    }

    when (stdout) {
        Stdio.Inherit -> pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        Stdio.Capture -> pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
        Stdio.Null -> pb.redirectOutput(ProcessBuilder.Redirect.to(File("/dev/null")))
        is Stdio.File -> pb.redirectOutput(ProcessBuilder.Redirect.appendTo(stdout.file))
    }

    when (stderr) {
        Stdio.Inherit -> pb.redirectError(ProcessBuilder.Redirect.INHERIT)
        Stdio.Capture -> pb.redirectError(ProcessBuilder.Redirect.PIPE)
        Stdio.Null -> pb.redirectError(ProcessBuilder.Redirect.to(File("/dev/null")))
        is Stdio.File -> pb.redirectError(ProcessBuilder.Redirect.appendTo(stderr.file))
    }

    return pb
}

/**
 * Defines the source of the process's standard input.
 */
sealed interface Stdin {

    /** The process inherits the parent process's standard input. */
    data object Inherit : Stdin

    /** Standard input is provided via a pipe that can be written to. */
    data object Pipe : Stdin

    /** Standard input is provided as a fixed [ByteArray]. */
    data class Bytes(val data: ByteArray) : Stdin {
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

@Deprecated("Use Stdin", ReplaceWith("Stdin"))
typealias StdinSource = Stdin

/**
 * Defines where the process's standard output or error should be directed.
 */
sealed interface Stdio {

    /** Output is inherited from the parent process. */
    data object Inherit : Stdio

    /** Output is captured and can be read as bytes or text. */
    data object Capture : Stdio

    /** Output is discarded (sent to `/dev/null`). */
    data object Null : Stdio

    /** Output is appended to the specified [file]. */
    data class File(val file: java.io.File) : Stdio
}

@Deprecated("Use Stdio", ReplaceWith("Stdio"))
typealias StdioDest = Stdio

/**
 * Captured results of a process execution.
 *
 * @property exitCode The process exit code (typically 0 for success).
 * @property stdout Captured standard output as bytes.
 * @property stderr Captured standard error as bytes.
 */
data class CommandResult(
    val exitCode: Int,
    val stdout: ByteArray,
    val stderr: ByteArray,
) {
    /** The captured standard output decoded as a UTF-8 string. */
    val stdoutText: String get() = stdout.decodeToString()

    /** The captured standard error decoded as a UTF-8 string. */
    val stderrText: String get() = stderr.decodeToString()

    /** The captured standard output split into lines. */
    val stdoutLines: List<String> get() = stdoutText.lines()

    /** The captured standard error split into lines. */
    val stderrLines: List<String> get() = stderrText.lines()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandResult

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

@Deprecated("Use CommandResult", ReplaceWith("CommandResult"))
typealias ProcessOutput = CommandResult

/**
 * Events emitted during a streaming process execution.
 */
sealed interface ProcessEvent {

    /** Standard output data chunk. */
    data class Stdout(val data: ByteArray) : ProcessEvent {
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
    data class Stderr(val data: ByteArray) : ProcessEvent {
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
    data class ExitCode(val code: Int) : ProcessEvent
}

@Deprecated("Use ProcessEvent", ReplaceWith("ProcessEvent"))
typealias ProcessOutputEvent = ProcessEvent

/**
 * A handle to a running process, providing methods to wait for its completion or terminate it.
 */
class ProcessHandle internal constructor(
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
    suspend fun waitFor(): CommandResult = coroutineScope {
        val out = async(Dispatchers.IO) { process.inputStream.readBytes() }
        val err = async(Dispatchers.IO) { process.errorStream.readBytes() }
        withContext(Dispatchers.IO) { process.waitFor() }
        CommandResult(process.exitValue(), out.await(), err.await())
    }

    /**
     * Suspends until the process completes or the [timeoutMillis] is reached.
     *
     * @return The [CommandResult] if the process finished, or null if it timed out.
     */
    suspend fun waitForTimeout(timeoutMillis: Long): CommandResult? = coroutineScope {
        val exited = withContext(Dispatchers.IO) {
            process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        }
        if (!exited) return@coroutineScope null
        val out = async(Dispatchers.IO) { process.inputStream.readBytes() }
        val err = async(Dispatchers.IO) { process.errorStream.readBytes() }
        CommandResult(process.exitValue(), out.await(), err.await())
    }

    /**
     * Suspends until the process completes or the [timeout] is reached.
     */
    suspend fun waitForTimeout(timeout: Duration) = waitForTimeout(timeout.inWholeMilliseconds)

    /**
     * Returns a [Flow] that emits stdout/stderr chunks and the final exit code as they occur.
     * The process is automatically forcibly destroyed if the flow collection is cancelled.
     */
    fun flow(): Flow<ProcessEvent> = channelFlow {
        val bufferSize = 8192

        val outJob = launch(Dispatchers.IO) {
            process.inputStream.use { stream ->
                val buffer = ByteArray(bufferSize)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    if (n > 0) send(ProcessEvent.Stdout(buffer.copyOf(n)))
                }
            }
        }

        val errJob = launch(Dispatchers.IO) {
            process.errorStream.use { stream ->
                val buffer = ByteArray(bufferSize)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    if (n > 0) send(ProcessEvent.Stderr(buffer.copyOf(n)))
                }
            }
        }

        try {
            withContext(Dispatchers.IO) { process.waitFor() }
            outJob.join()
            errJob.join()
            send(ProcessEvent.ExitCode(process.exitValue()))
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    /** Forcibly kills the child process. */
    fun kill() {
        process.destroyForcibly()
    }

    /** Gracefully terminates the child process. */
    fun terminate() = process.destroy()
}

@Deprecated("Use ProcessHandle", ReplaceWith("ProcessHandle"))
typealias ChildProcess = ProcessHandle

/**
 * A pipeline of commands where the stdout of each feeds the stdin of the next.
 *
 * The exit code is that of the last command. Intermediate stdout and stderr are
 * piped automatically. Built via [Command.pipe].
 */
class Pipeline internal constructor(
    val commands: List<Command>,
) {
    /**
     * Executes the pipeline and returns the output of the last command.
     * All intermediate processes are piped together and cleaned up on completion.
     */
    suspend fun execute(): CommandResult = coroutineScope {
        if (commands.isEmpty()) error("Empty pipeline")
        val processes = mutableListOf<Process>()
        try {
            for ((i, cmd) in commands.withIndex()) {
                val isFirst = i == 0
                val isLast = i == commands.lastIndex
                val process = createProcess(
                    program = cmd.program,
                    args = cmd.args,
                    env = cmd.env,
                    cwd = cmd.cwd,
                    stdin = if (isFirst) cmd.stdinSource else Stdin.Pipe,
                    stdout = if (isLast) cmd.stdoutDest else Stdio.Capture,
                    stderr = cmd.stderrDest,
                )
                processes.add(process)
            }

            for (i in 0 until processes.size - 1) {
                launch(Dispatchers.IO) {
                    try {
                        processes[i].inputStream.use { out ->
                            processes[i + 1].outputStream.use { `in` -> out.copyTo(`in`) }
                        }
                    } catch (_: IOException) {
                    }
                }
            }

            val firstCmd = commands.first()
            if (firstCmd.stdinSource is Stdin.Bytes) {
                val bytes = (firstCmd.stdinSource as Stdin.Bytes).data
                launch(Dispatchers.IO) {
                    processes.first().outputStream.use { it.write(bytes) }
                }
            }

            val lastCmd = commands.last()
            val lastProc = processes.last()
            val outDeferred = if (lastCmd.stdoutDest == Stdio.Capture) {
                async(Dispatchers.IO) { lastProc.inputStream.readBytes() }
            } else null
            val errDeferred = if (lastCmd.stderrDest == Stdio.Capture) {
                async(Dispatchers.IO) { lastProc.errorStream.readBytes() }
            } else null

            withContext(Dispatchers.IO) { lastProc.waitFor() }

            CommandResult(
                exitCode = lastProc.exitValue(),
                stdout = outDeferred?.await() ?: EMPTY_BYTE_ARRAY,
                stderr = errDeferred?.await() ?: EMPTY_BYTE_ARRAY,
            )
        } finally {
            processes.forEach { if (it.isAlive) it.destroyForcibly() }
        }
    }

    /**
     * Executes the pipeline and returns a stream of events from the last command.
     * Intermediate stdout is piped to the next command's stdin. Stderr from all
     * commands uses each command's configured [Command.stderr].
     */
    fun watch(): Flow<ProcessEvent> = channelFlow {
        if (commands.isEmpty()) error("Empty pipeline")
        val processes = mutableListOf<Process>()
        try {
            for ((i, cmd) in commands.withIndex()) {
                val isFirst = i == 0
                val process = createProcess(
                    program = cmd.program,
                    args = cmd.args,
                    env = cmd.env,
                    cwd = cmd.cwd,
                    stdin = if (isFirst) cmd.stdinSource else Stdin.Pipe,
                    stdout = Stdio.Capture,
                    stderr = cmd.stderrDest,
                )
                processes.add(process)
            }

            for (i in 0 until processes.size - 1) {
                launch(Dispatchers.IO) {
                    try {
                        processes[i].inputStream.use { out ->
                            processes[i + 1].outputStream.use { `in` -> out.copyTo(`in`) }
                        }
                    } catch (_: IOException) { }
                }
            }

            val firstCmd = commands.first()
            if (firstCmd.stdinSource is Stdin.Bytes) {
                launch(Dispatchers.IO) {
                    processes.first().outputStream.use { it.write((firstCmd.stdinSource as Stdin.Bytes).data) }
                }
            }

            val last = processes.last()
            val lastCmd = commands.last()

            val outJob = if (lastCmd.stdoutDest == Stdio.Capture) {
                launch(Dispatchers.IO) {
                    last.inputStream.use { stream ->
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = stream.read(buf)
                            if (n < 0) break
                            if (n > 0) send(ProcessEvent.Stdout(buf.copyOf(n)))
                        }
                    }
                }
            } else null

            val errJob = if (lastCmd.stderrDest == Stdio.Capture) {
                launch(Dispatchers.IO) {
                    last.errorStream.use { stream ->
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = stream.read(buf)
                            if (n < 0) break
                            if (n > 0) send(ProcessEvent.Stderr(buf.copyOf(n)))
                        }
                    }
                }
            } else null

            withContext(Dispatchers.IO) { last.waitFor() }
            outJob?.join()
            errJob?.join()
            send(ProcessEvent.ExitCode(last.exitValue()))
        } finally {
            processes.forEach { if (it.isAlive) it.destroyForcibly() }
        }
    }

    /** Appends [next] to this pipeline. */
    infix fun pipe(next: Command): Pipeline = Pipeline(commands + next)
}

/** Creates a [Pipeline] where this command's stdout is piped to [next]'s stdin. */
infix fun Command.pipe(next: Command): Pipeline = Pipeline(listOf(this, next))

/**
 * Shell-escapes a single argument for inclusion in a `bash -c` command string.
 * Wraps in single quotes and handles embedded single quotes via the `'\''` idiom.
 */
private fun shellEscape(arg: String): String = buildString {
    append('\'')
    for (ch in arg) {
        if (ch == '\'') {
            append("'\\''")
        } else {
            append(ch)
        }
    }
    append('\'')
}

/**
 * Represents a program that has been resolved to a specific execution path.
 */
sealed interface ResolvedProgram {

    /** A binary that can be executed directly by the system. */
    data class Direct(val path: String) : ResolvedProgram

    /** A binary that must be executed within the PRoot environment. */
    data class PRoot(val path: String, val guestPath: String = path) : ResolvedProgram
}

internal val ROOTFS_BIN_PATHS = listOf(
    "/usr/local/bin", "/usr/bin", "/bin",
    "/usr/local/sbin", "/usr/sbin", "/sbin"
)

internal val HOME_BIN_PATHS = listOf(
    ".local/bin", ".bin", "bin"
)

internal val SYSTEM_BIN_PATHS = listOf(
    "/system/bin", "/system/xbin", "/vendor/bin",
)

internal suspend fun resolveProgram(program: String): ResolvedProgram {
    val rootFs = try {
        Paths.rootFs.takeIf { it.exists() }
    } catch (_: Exception) {
        null
    }
    val home = try {
        Paths.home.takeIf { it.exists() }
    } catch (_: Exception) {
        null
    }

    if (program.contains(File.separatorChar)) {
        val f = File(program)
        val absPath = f.absolutePath

        if (rootFs != null && absPath.startsWith(rootFs.absolutePath)) {
            val guestPath = "/" + absPath.substring(rootFs.absolutePath.length).trimStart('/')
            return ResolvedProgram.PRoot(absPath, guestPath)
        }
        if (home != null && absPath.startsWith(home.absolutePath)) {
            val rel = absPath.substring(home.absolutePath.length).trimStart('/')
            return ResolvedProgram.PRoot(absPath, "/root/$rel")
        }

        if (home != null && program.startsWith("/root/")) {
            val rel = program.substringAfter("/root/")
            val inHome = home.resolve(rel)
            if (inHome.exists()) {
                return ResolvedProgram.PRoot(inHome.absolutePath, "/root/$rel")
            }
        }

        if (rootFs != null && program.startsWith("/")) {
            val inRootfs = rootFs.resolve(program.trimStart('/'))
            if (inRootfs.exists()) {
                val guestPath = "/" + program.trimStart('/')
                return ResolvedProgram.PRoot(inRootfs.absolutePath, guestPath)
            }
        }

        return ResolvedProgram.Direct(program)
    }

    // Simple name - priority to system paths for things like 'sh', 'ls' if we want host versions?
    // Actually, usually in Klyx we want the terminal versions if they exist.

    // Search in system paths first for Direct execution
    for (dir in SYSTEM_BIN_PATHS) {
        val f = File(dir, program)
        if (f.exists()) {
            return ResolvedProgram.Direct(f.absolutePath)
        }
    }

    if (rootFs != null) {
        for (dir in ROOTFS_BIN_PATHS) {
            val f = rootFs.resolve(dir.trimStart('/')).resolve(program)
            if (f.exists()) {
                return ResolvedProgram.PRoot(f.absolutePath)
            }
        }
    }

    if (home != null) {
        for (dir in HOME_BIN_PATHS) {
            val f = home.resolve(dir).resolve(program)
            if (f.exists()) {
                return ResolvedProgram.PRoot(f.absolutePath, "/root/$dir/$program")
            }
        }
    }

    // Final fallback. When the terminal rootfs is installed, assume the program lives inside it
    // and execute via PRoot. Otherwise the terminal isn't installed. fabricating a PRoot
    // execution would only fail with a confusing proot binding error, so fall back to a direct
    // exec attempt and let the failure surface as a clear "command not found" (IOException).
    return if (rootFs == null) ResolvedProgram.Direct(program) else ResolvedProgram.PRoot(program)
}
