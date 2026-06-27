package com.klyx.system

import com.klyx.data.fs.Paths
import com.klyx.terminal.rootFs
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

fun shell(script: String): CommandBuilder = command("sh", "-c", script)

suspend fun commandExists(program: String): Boolean = which(program) != null

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

suspend fun firstAvailable(vararg commands: String): String? {
    for (cmd in commands) {
        val path = which(cmd)
        if (path != null) return path
    }
    return null
}

suspend fun firstAvailable(commands: Iterable<String>): String? {
    for (cmd in commands) {
        val path = which(cmd)
        if (path != null) return path
    }
    return null
}

suspend fun CommandBuilder.outputText(): String = output().stdoutText
suspend fun CommandBuilder.outputLines(): List<String> = outputText().lines()

suspend fun CommandBuilder.isSuccess(): Boolean = status() == 0
suspend fun CommandBuilder.isFailure(): Boolean = status() != 0

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

suspend fun CommandBuilder.retry(times: Int = 3): ProcessOutput {
    var result = output()
    repeat(times) {
        if (result.exitCode == 0) return result
        result = output()
    }
    return result
}

suspend fun CommandBuilder.result(): Result<ProcessOutput> = runCatching { output() }

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

suspend fun ChildProcess.waitForText(): String = waitFor().stdoutText
suspend fun ChildProcess.waitForLines(): List<String> = waitForText().lines()

suspend fun ChildProcess.waitForTimeoutText(timeout: Duration): String? =
    waitForTimeout(timeout)?.stdoutText

suspend fun ChildProcess.waitForTimeoutLines(timeout: Duration): List<String>? =
    waitForTimeoutText(timeout)?.lines()

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

suspend fun CommandBuilder.combinedLines(): Flow<String> = stream().combinedLines()

fun ChildProcess.combinedLines(): Flow<String> = flow().combinedLines()

fun Flow<ProcessOutputEvent>.stdoutBytes(): Flow<ByteArray> = mapNotNull {
    (it as? ProcessOutputEvent.Stdout)?.data
}

fun Flow<ProcessOutputEvent>.stderrBytes(): Flow<ByteArray> = mapNotNull {
    (it as? ProcessOutputEvent.Stderr)?.data
}

suspend fun CommandBuilder.stdoutBytes(): Flow<ByteArray> = stream().stdoutBytes()

suspend fun CommandBuilder.stderrBytes(): Flow<ByteArray> = stream().stderrBytes()

fun ChildProcess.stdoutBytes(): Flow<ByteArray> = flow().stdoutBytes()

fun ChildProcess.stderrBytes(): Flow<ByteArray> = flow().stderrBytes()

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

suspend fun CommandBuilder.streamLines(): Flow<String> = stream().stdoutLines()
suspend fun CommandBuilder.streamErrLines(): Flow<String> = stream().stderrLines()

fun ChildProcess.streamLines(): Flow<String> = flow().stdoutLines()
fun ChildProcess.streamErrLines(): Flow<String> = flow().stderrLines()
