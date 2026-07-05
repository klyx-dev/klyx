package com.klyx.api.system

import com.klyx.api.data.fs.Paths
import com.klyx.api.terminal.home
import com.klyx.api.terminal.rootFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * Creates a [CommandBuilder] that executes the given [script] in a shell (`sh -c`).
 */
fun shell(script: String): CommandBuilder = command("sh", "-c", script)

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
        command("which", program)
            .stdout(StdioDest.Capture)
            .stderr(StdioDest.Null)
            .outputText()
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
suspend fun CommandBuilder.outputText(): String = output().stdoutText

/** Executes the command and returns the captured standard output as a list of lines. */
suspend fun CommandBuilder.outputLines(): List<String> = outputText().lines()

/** Executes the command and returns `true` if the exit code is 0. */
suspend fun CommandBuilder.isSuccess(): Boolean = status() == 0

/** Executes the command and returns `true` if the exit code is non-zero. */
suspend fun CommandBuilder.isFailure(): Boolean = status() != 0

/**
 * Executes the command and waits for it to finish, with a specified [timeout].
 *
 * @return The [ProcessOutput] if finished within timeout, or null if it timed out.
 */
suspend fun CommandBuilder.outputWithTimeout(timeout: Duration): ProcessOutput? {
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
suspend fun CommandBuilder.retry(times: Int = 3): ProcessOutput {
    var result = output()
    repeat(times) {
        if (result.exitCode == 0) return result
        result = output()
    }
    return result
}

/**
 * Executes the command and returns the result as a [Result] wrapping [ProcessOutput].
 */
suspend fun CommandBuilder.result(): Result<ProcessOutput> = runCatching { output() }

/**
 * Pipes the standard output of this command into the standard input of the [destination] command.
 *
 * @return The [ChildProcess] handle for the destination command.
 */
suspend fun CommandBuilder.pipeTo(destination: CommandBuilder): ChildProcess {
    val src = stdout(StdioDest.Capture).spawn()
    val dest = destination.stdin(StdinSource.Pipe).spawn()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            src.stdout.use { out ->
                dest.stdin.use { `in` ->
                    out.copyTo(`in`)
                }
            }
        } catch (_: IOException) {
        }
    }
    return dest
}

/** Suspends until the process completes and returns the stdout decoded as text. */
suspend fun ChildProcess.waitForText(): String = waitFor().stdoutText

/** Suspends until the process completes and returns the stdout split into lines. */
suspend fun ChildProcess.waitForLines(): List<String> = waitForText().lines()

/**
 * Suspends until the process completes or [timeout] is reached, returning stdout text.
 */
suspend fun ChildProcess.waitForTimeoutText(timeout: Duration): String? =
    waitForTimeout(timeout)?.stdoutText

/**
 * Suspends until the process completes or [timeout] is reached, returning stdout split into lines.
 */
suspend fun ChildProcess.waitForTimeoutLines(timeout: Duration): List<String>? =
    waitForTimeoutText(timeout)?.lines()

/**
 * Merges standard output and standard error events into a single stream of lines.
 */
fun Flow<ProcessOutputEvent>.combinedLines(): Flow<String> = channelFlow {
    val buffer = StringBuilder()
    collect { event ->
        when (event) {
            is ProcessOutputEvent.Stdout -> buffer.append(event.text)
            is ProcessOutputEvent.Stderr -> buffer.append(event.text)
            is ProcessOutputEvent.ExitCode -> {}
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
suspend fun CommandBuilder.combinedLines(): Flow<String> = stream().combinedLines()

/**
 * Streams both standard output and error merged as a stream of lines from a running process.
 */
fun ChildProcess.combinedLines(): Flow<String> = flow().combinedLines()

/**
 * Filters a process event stream to only emit standard output as byte arrays.
 */
fun Flow<ProcessOutputEvent>.stdoutBytes(): Flow<ByteArray> = mapNotNull {
    (it as? ProcessOutputEvent.Stdout)?.data
}

/**
 * Filters a process event stream to only emit standard error as byte arrays.
 */
fun Flow<ProcessOutputEvent>.stderrBytes(): Flow<ByteArray> = mapNotNull {
    (it as? ProcessOutputEvent.Stderr)?.data
}

/**
 * Streams the process standard output as a sequence of byte arrays.
 */
suspend fun CommandBuilder.stdoutBytes(): Flow<ByteArray> = stream().stdoutBytes()

/**
 * Streams the process standard error as a sequence of byte arrays.
 */
suspend fun CommandBuilder.stderrBytes(): Flow<ByteArray> = stream().stderrBytes()

/**
 * Streams the running process's standard output as byte arrays.
 */
fun ChildProcess.stdoutBytes(): Flow<ByteArray> = flow().stdoutBytes()

/**
 * Streams the running process's standard error as byte arrays.
 */
fun ChildProcess.stderrBytes(): Flow<ByteArray> = flow().stderrBytes()

/**
 * Filters a process event stream to only emit standard output as a stream of lines.
 */
fun Flow<ProcessOutputEvent>.stdoutLines(): Flow<String> = channelFlow {
    val buffer = StringBuilder()
    collect { event ->
        if (event is ProcessOutputEvent.Stdout) {
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
fun Flow<ProcessOutputEvent>.stderrLines(): Flow<String> = channelFlow {
    val buffer = StringBuilder()
    collect { event ->
        if (event is ProcessOutputEvent.Stderr) {
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
suspend fun CommandBuilder.streamLines(): Flow<String> = stream().stdoutLines()

/**
 * Streams the command's standard error as a stream of lines.
 */
suspend fun CommandBuilder.streamErrLines(): Flow<String> = stream().stderrLines()

/**
 * Streams the running process's standard output as a stream of lines.
 */
fun ChildProcess.streamLines(): Flow<String> = flow().stdoutLines()

/**
 * Streams the running process's standard error as a stream of lines.
 */
fun ChildProcess.streamErrLines(): Flow<String> = flow().stderrLines()
