package com.klyx.core.process

import com.klyx.core.io.stripSandboxRoot
import io.itsvks.anyhow.anyhow
import kotlinx.io.files.Path
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Command(val command: String) {
    private val _args = mutableListOf<String>()
    private val _env = mutableMapOf<String, String>()
    private var directory: Path? = null

    val args get() = _args.toList()

    companion object {
        @JvmStatic
        fun newCommand(path: Path, resolveForAndroid: Boolean = true): Command {
            val path = if (resolveForAndroid) path.stripSandboxRoot() else path
            return Command(path.toString())
        }

        @JvmStatic
        fun newCommand(command: String) = Command(command)
    }

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
    suspend fun output(timeoutMillis: Long) = anyhow {
        val raw = systemProcess(command, *_args.toTypedArray()) {
            environment { putAll(_env) }
            directory?.let { changeDir(it) }
        }.output {
            this.timeoutMillis = timeoutMillis.toInt()
        }

        if (raw.processError != null) {
            systemProcessLogger.warn { "Process error: ${raw.processError}" }
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
                append("directory: $it\n")
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
