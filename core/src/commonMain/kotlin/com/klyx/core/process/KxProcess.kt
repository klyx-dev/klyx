@file:OptIn(ExperimentalContracts::class)

package com.klyx.core.process

import io.matthewnelson.kmp.process.Output
import io.matthewnelson.kmp.process.OutputFeed
import io.matthewnelson.kmp.process.Process
import io.matthewnelson.kmp.process.ProcessException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
private annotation class KxProcessDsl

@Retention(AnnotationRetention.BINARY)
@RequiresOptIn
annotation class InternalProcessApi

typealias Signal = io.matthewnelson.kmp.process.Signal
//typealias Output = io.matthewnelson.kmp.process.Output

expect val SystemPathSeparator: Char

@KxProcessDsl
typealias KxProcessBuilder = Process.Builder
typealias KxProcessException = ProcessException

typealias CurrentProcess = Process.Current

fun KxProcessBuilder.arg(arg: String) = args(arg)
fun KxProcessBuilder.arg(arg: Path) = arg(arg.toString())

fun KxProcessBuilder.args(vararg args: Path) = args(args.map(Path::toString))
fun KxProcessBuilder.args(args: Iterable<Path>) = args(args.map(Path::toString))

expect fun KxProcessBuilder.changeDir(dir: Path)

class KxProcess @PublishedApi internal constructor(internal val builder: KxProcessBuilder) {
    @PublishedApi
    internal lateinit var process: Process
    val handler get() = withProcess { this as OutputFeed.Handler }

    @InternalProcessApi
    val raw get() = withProcess { this }

    inline val startTime: ComparableTimeMark
        get() {
            checkProcessSpawned()
            return process.startTime
        }

    inline val isAlive get() = exitCodeOrNull() == null
    val isSpawned get() = ::process.isInitialized

    fun spawn(): KxProcess {
        if (!isSpawned) {
            process = builder.createProcess()
        }
        return this
    }

    fun destroy() = withProcess { destroy(); this }
    fun exitCode() = withProcess { exitCode() }
    fun exitCodeOrNull() = withProcess { exitCodeOrNull() }
    fun pid() = withProcess { pid() }

    suspend fun output() =
        withContext(Dispatchers.Default) { builder.createOutput { timeoutMillis = 5000 } }

    suspend fun output(block: Output.Options.Builder.() -> Unit) =
        withContext(Dispatchers.Default) { builder.createOutput(block) }

    suspend fun waitFor() = withProcess { waitForAsync() }
    suspend fun waitFor(duration: Duration) = withProcess { waitForAsync(duration) }

    private inline fun <R> withProcess(block: Process.() -> R): R {
        checkProcessSpawned()
        return process.block()
    }

    @PublishedApi
    internal fun checkProcessSpawned() = check(::process.isInitialized) { "Process has not been spawned" }

    override fun toString(): String {
        return if (::process.isInitialized) {
            process.toString()
        } else {
            "KxProcess(Process not spawned)"
        }
    }
}

inline fun <T> KxProcess.use(block: (process: KxProcess) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val p = if (isSpawned) this else spawn()

    val result = try {
        block(p)
    } catch (t: Throwable) {
        destroy()
        throw t
    }

    destroy()
    return result
}

inline fun process(command: String, block: KxProcessBuilder.() -> Unit = {}): KxProcess {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return KxProcess(Process.Builder(command).apply(block))
}

inline fun process(commands: Array<out String>, block: KxProcessBuilder.() -> Unit = {}): KxProcess {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    require(commands.isNotEmpty()) { "commands cannot be empty" }
    return process(commands.first()) { args(commands.drop(1)); block() }
}

@JvmName("process0")
inline fun process(vararg commands: String, block: KxProcessBuilder.() -> Unit = {}): KxProcess {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    return if (commands.size == 1) {
        val command = commands[0]
        process(command, block)
    } else {
        process(commands, block)
    }
}

inline fun process(path: Path, block: KxProcessBuilder.() -> Unit = {}) = process(path.toString(), block)

@Suppress("NOTHING_TO_INLINE")
inline fun currentProcess() = CurrentProcess
