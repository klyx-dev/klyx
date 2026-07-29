@file:Suppress("unused")

package com.klyx.api.system

import com.klyx.api.data.fs.Paths
import com.klyx.api.terminal.home
import com.klyx.api.terminal.rootFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import kotlin.time.Duration

/**
 * Creates a [Command] that executes the given [script] in a shell (`sh -c`).
 */
@Deprecated("Use Command.shell()", ReplaceWith("Command.shell(script)"))
fun shell(script: String): Command = Command.shell(script)

/**
 * Returns `true` if the given [program] can be found in the current environment's path.
 */
suspend fun commandExists(program: String): Boolean = which(program) != null

/**
 * Searches for the absolute path of the given [program].
 *
 * It checks the terminal rootfs binaries first, then standard Android system paths.
 * If not found manually, it falls back to the system `which` command.
 */
suspend fun which(program: String): String? = withContext(Dispatchers.IO) {
    try {
        if (program.contains(File.separatorChar)) {
            val f = File(program)
            if (f.exists() && f.canExecute()) return@withContext f.absolutePath
            return@withContext null
        }
        val rf = try {
            Paths.rootFs.takeIf { it.exists() }
        } catch (_: Exception) {
            null
        }
        if (rf != null) {
            for (dir in ROOTFS_BIN_PATHS) {
                val f = rf.resolve(dir.trimStart('/')).resolve(program)
                if (f.exists() && f.canExecute()) return@withContext f.absolutePath
            }
        }
        val home = try {
            Paths.home.takeIf { it.exists() }
        } catch (_: Exception) {
            null
        }
        if (home != null) {
            for (dir in HOME_BIN_PATHS) {
                val f = home.resolve(dir).resolve(program)
                if (f.exists() && f.canExecute()) return@withContext f.absolutePath
            }
        }
        for (dir in SYSTEM_BIN_PATHS) {
            val f = File(dir, program)
            if (f.exists() && f.canExecute()) return@withContext f.absolutePath
        }
    } catch (_: Exception) {
    }

    runCatching {
        command("bash", "-lc", "command -v $program")
            .stdout(Stdio.Capture)
            .stderr(Stdio.Null)
            .output().stdoutText
            .trim()
            .ifEmpty { null }
    }.getOrNull()
}

/**
 * Returns the absolute path of the first available command from the provided [commands].
 */
suspend fun firstAvailable(vararg commands: String): String? {
    for (cmd in commands) {
        val path = which(cmd)
        if (path != null) return path
    }
    return null
}

/**
 * Returns the absolute path of the first available command from the provided [commands] iterable.
 */
suspend fun firstAvailable(commands: Iterable<String>): String? {
    for (cmd in commands) {
        val path = which(cmd)
        if (path != null) return path
    }
    return null
}

/** Executes the command and returns the captured standard output as a string. */
@Deprecated("Use output().stdoutText", ReplaceWith("output().stdoutText"))
suspend fun Command.outputText(): String = output().stdoutText

/** Executes the command and returns the captured standard output as a list of lines. */
@Deprecated("Use output().stdoutLines", ReplaceWith("output().stdoutLines"))
suspend fun Command.outputLines(): List<String> = output().stdoutText.lines()

/** Executes the command and returns `true` if the exit code is 0. */
suspend fun Command.isSuccess(): Boolean = status() == 0

/** Executes the command and returns `true` if the exit code is non-zero. */
suspend fun Command.isFailure(): Boolean = status() != 0

/**
 * Executes the command and waits for it to finish, with a specified [timeout].
 *
 * @return The [CommandResult] if finished within timeout, or null if it timed out.
 */
suspend fun Command.outputWithTimeout(timeout: Duration): CommandResult? {
    val child = spawn()
    return try {
        withTimeoutOrNull(timeout) {
            child.waitFor()
        }
    } finally {
        if (child.isRunning) child.kill()
    }
}

/**
 * Executes the command and retries up to [times] if it fails (non-zero exit code).
 */
suspend fun Command.retry(times: Int = 3): CommandResult {
    var result = output()
    repeat(times) {
        if (result.exitCode == 0) return result
        result = output()
    }
    return result
}

/**
 * Executes the command and returns the result as a [Result] wrapping [CommandResult].
 */
suspend fun Command.result(): Result<CommandResult> = runCatching { output() }

/**
 * Pipes the standard output of this command into the standard input of the [destination] command.
 *
 * @return The [ProcessHandle] handle for the destination command.
 */
suspend fun Command.pipeTo(destination: Command): ProcessHandle {
    val srcProcess = createProcess(
        program = program,
        args = args,
        env = env,
        cwd = cwd,
        stdin = stdinSource,
        stdout = Stdio.Capture,
        stderr = stderrDest,
    )
    val destProcess = createProcess(
        program = destination.program,
        args = destination.args,
        env = destination.env,
        cwd = destination.cwd,
        stdin = Stdin.Pipe,
        stdout = destination.stdoutDest,
        stderr = destination.stderrDest,
    )

    val parentJob = currentCoroutineContext()[Job]
    CoroutineScope(Dispatchers.IO + (parentJob ?: SupervisorJob())).launch {
        try {
            srcProcess.inputStream.use { out ->
                destProcess.outputStream.use { `in` ->
                    out.copyTo(`in`)
                }
            }
        } catch (_: IOException) {
        }
    }
    return ProcessHandle(destProcess)
}

/** Suspends until the process completes and returns the stdout decoded as text. */
suspend fun ProcessHandle.waitForText(): String = waitFor().stdoutText

/** Suspends until the process completes and returns the stdout split into lines. */
suspend fun ProcessHandle.waitForLines(): List<String> = waitForText().lines()

/**
 * Suspends until the process completes or [timeout] is reached, returning stdout text.
 */
suspend fun ProcessHandle.waitForTimeoutText(timeout: Duration): String? =
    waitForTimeout(timeout)?.stdoutText

/**
 * Suspends until the process completes or [timeout] is reached, returning stdout split into lines.
 */
suspend fun ProcessHandle.waitForTimeoutLines(timeout: Duration): List<String>? =
    waitForTimeoutText(timeout)?.lines()

/**
 * Merges standard output and standard error events into a single stream of lines.
 */
fun Flow<ProcessEvent>.combinedLines(): Flow<String> = channelFlow {
    val buffer = StringBuilder()
    collect { event ->
        when (event) {
            is ProcessEvent.Stdout -> buffer.append(event.text)
            is ProcessEvent.Stderr -> buffer.append(event.text)
            is ProcessEvent.ExitCode -> {}
        }
        while (true) {
            val idx = buffer.indexOf("\n")
            if (idx == -1) break
            send(buffer.substring(0, idx))
            buffer.delete(0, idx + 1)
        }
    }
    if (buffer.isNotEmpty()) {
        send(buffer.toString())
    }
}

/**
 * Streams both standard output and error merged as a stream of lines.
 */
suspend fun Command.combinedLines(): Flow<String> = stream().combinedLines()

/**
 * Streams both standard output and error merged as a stream of lines from a running process.
 */
fun ProcessHandle.combinedLines(): Flow<String> = flow().combinedLines()

/**
 * Filters a process event stream to only emit standard output as byte arrays.
 */
fun Flow<ProcessEvent>.stdoutBytes(): Flow<ByteArray> = mapNotNull {
    (it as? ProcessEvent.Stdout)?.data
}

/**
 * Filters a process event stream to only emit standard error as byte arrays.
 */
fun Flow<ProcessEvent>.stderrBytes(): Flow<ByteArray> = mapNotNull {
    (it as? ProcessEvent.Stderr)?.data
}

/**
 * Streams the process standard output as a sequence of byte arrays.
 */
suspend fun Command.stdoutBytes(): Flow<ByteArray> = stream().stdoutBytes()

/**
 * Streams the process standard error as a sequence of byte arrays.
 */
suspend fun Command.stderrBytes(): Flow<ByteArray> = stream().stderrBytes()

/**
 * Streams the running process's standard output as byte arrays.
 */
fun ProcessHandle.stdoutBytes(): Flow<ByteArray> = flow().stdoutBytes()

/**
 * Streams the running process's standard error as byte arrays.
 */
fun ProcessHandle.stderrBytes(): Flow<ByteArray> = flow().stderrBytes()

/**
 * Filters a process event stream to only emit standard output as a stream of lines.
 */
fun Flow<ProcessEvent>.stdoutLines(): Flow<String> = channelFlow {
    val buffer = StringBuilder()
    collect { event ->
        if (event is ProcessEvent.Stdout) {
            buffer.append(event.text)
            while (true) {
                val idx = buffer.indexOf("\n")
                if (idx == -1) break
                send(buffer.substring(0, idx))
                buffer.delete(0, idx + 1)
            }
        }
    }
    if (buffer.isNotEmpty()) {
        send(buffer.toString())
    }
}

/**
 * Filters a process event stream to only emit standard error as a stream of lines.
 */
fun Flow<ProcessEvent>.stderrLines(): Flow<String> = channelFlow {
    val buffer = StringBuilder()
    collect { event ->
        if (event is ProcessEvent.Stderr) {
            buffer.append(event.text)
            while (true) {
                val idx = buffer.indexOf("\n")
                if (idx == -1) break
                send(buffer.substring(0, idx))
                buffer.delete(0, idx + 1)
            }
        }
    }
    if (buffer.isNotEmpty()) {
        send(buffer.toString())
    }
}

/**
 * Streams the command's standard output as a stream of lines.
 */
suspend fun Command.streamLines(): Flow<String> = stream().stdoutLines()

/**
 * Streams the command's standard error as a stream of lines.
 */
suspend fun Command.streamErrLines(): Flow<String> = stream().stderrLines()

/**
 * Streams the running process's standard output as a stream of lines.
 */
fun ProcessHandle.streamLines(): Flow<String> = flow().stdoutLines()

/**
 * Streams the running process's standard error as a stream of lines.
 */
fun ProcessHandle.streamErrLines(): Flow<String> = flow().stderrLines()
