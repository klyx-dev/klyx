package com.klyx.core.process

import arrow.core.raise.result
import com.klyx.core.io.stripSandboxRoot
import io.matthewnelson.kmp.process.Stdio
import io.matthewnelson.kmp.process.Stdio.Pipe
import kotlinx.io.files.Path
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Command(val command: String) {
    private val _args = mutableListOf<String>()
    private val _env = mutableMapOf<String, String>()
    private var directory: Path? = null

    private var stdin: Stdio = Pipe
    private var stdout: Stdio = Pipe
    private var stderr: Stdio = Pipe

    val args: List<String> get() = _args
    val env: Map<String, String> get() = _env

    companion object {
        @JvmStatic
        fun newCommand(path: Path, resolveForAndroid: Boolean = true): Command {
            val path = if (resolveForAndroid) path.stripSandboxRoot() else path
            return Command(path.toString())
        }

        @JvmStatic
        fun newCommand(command: String) = Command(command)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun component1() = command

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun component2() = args

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun component3() = env

    fun stdin(stdin: Stdio) = apply { this.stdin = stdin }
    fun stdout(stdout: Stdio) = apply { this.stdout = stdout }
    fun stderr(stderr: Stdio) = apply { this.stderr = stderr }

    fun arg(arg: String) = apply {
        _args += arg
    }

    fun arg(arg: Path) = apply {
        _args += arg.toString()
    }

    fun args(vararg args: String) = apply {
        this._args.addAll(args)
    }

    fun args(args: Iterable<String>) = apply {
        this._args.addAll(args)
    }

    fun args(vararg args: Path) = apply {
        this._args.addAll(args.map(Path::toString))
    }

    @JvmName("argsPath")
    fun args(args: Iterable<Path>) = apply {
        this._args.addAll(args.map(Path::toString))
    }

    fun env(key: String, value: String) = apply {
        _env += key to value
    }

    fun env(from: Map<String, String>) = apply {
        _env += from
    }

    fun env(block: MutableMap<String, String>.() -> Unit) = apply {
        _env += buildMap(block)
    }

    fun clearEnv() = apply {
        _env.clear()
    }

    fun currentDir(dir: Path) = apply {
        directory = dir
    }

    suspend fun output() = output(5.seconds)

    @JvmName("outputWithTimeoutDuration")
    suspend fun output(timeout: Duration) = output(timeout.inWholeMilliseconds)

    @JvmName("outputWithTimeout")
    suspend fun output(timeoutMillis: Long) = result {
        val raw = systemProcess(command, *_args.toTypedArray()) {
            environment { putAll(_env) }
            directory?.let { changeDir(it) }
            stdin(stdin)
            stdout(stdout)
            stderr(stderr)
        }.output {
            this.timeoutMillis = timeoutMillis.toInt()
        }

        if (raw.processError != null) {
            systemProcessLogger.warn { "Process error: ${raw.processError}\nCommand: ${this@Command}" }
        }

        Output(
            stdout = raw.stdout,
            stderr = raw.stderr,
            exitCode = raw.processInfo.exitCode,
            pid = raw.processInfo.pid,
            processError = raw.processError
        )
    }

    override fun toString(): String {
        return buildString {
            append(command)
            _args.forEach { arg ->
                append(" ")
                append(arg)
            }
            append("\n")
            directory?.let {
                append("  directory: $it\n")
            }
        }
    }
}

data class Output(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val pid: Int,
    val processError: String?
) {
    inline val isSuccess get() = exitCode == 0

    override fun toString(): String {
        return "Output(stdout='$stdout', stderr='$stderr', exitCode=$exitCode, pid=$pid, processError=$processError, isSuccess=$isSuccess)"
    }
}
