package com.klyx.wasm

import android.util.Log
import com.dylibso.chicory.log.Logger
import com.dylibso.chicory.wasi.WasiOptions
import com.klyx.nullInputStream
import com.klyx.nullOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@WasmDsl
internal class WasiOptionScope {
    private var random: Random = ThreadLocalRandom.current()
    private var clock = Clock.systemUTC()
    private var stdout = nullOutputStream()
    private var stderr = nullOutputStream()
    private var stdin = nullInputStream()
    private var arguments = listOf<String>()
    private var environment = mapOf<String, String>()
    private var directories = mapOf<String, String>()

    fun random(random: Random) {
        this.random = random
    }

    fun clock(clock: Clock) {
        this.clock = clock
    }

    fun stdout(stdout: OutputStream) {
        this.stdout = stdout
    }

    fun stderr(stderr: OutputStream) {
        this.stderr = stderr
    }

    fun stdin(stdin: InputStream) {
        this.stdin = stdin
    }

    fun inheritSystem() {
        this.stdout = System.out
        this.stdin = System.`in`
        this.stderr = System.err
    }

    fun arguments(arguments: List<String>) {
        this.arguments = arguments
    }

    fun environment(environment: Map<String, String>) {
        this.environment = environment
    }

    fun directories(directories: Map<String, String>) {
        this.directories = directories
    }

    fun build() = WasiOptions::class.java.getDeclaredConstructor(
        Random::class.java,
        Clock::class.java,
        OutputStream::class.java,
        OutputStream::class.java,
        InputStream::class.java,
        List::class.java,
        Map::class.java,
        Map::class.java
    ).apply { isAccessible = true }.newInstance(
        random, clock, stdout, stderr, stdin, arguments, environment, directories
    )
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
internal inline fun wasiOptions(
    @BuilderInference
    block: WasiOptionScope.() -> Unit
): WasiOptions {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return WasiOptionScope().apply(block).build()
}

object WasiLogger : Logger {
    private const val TAG = "KlyxWasi"

    override fun log(
        level: Logger.Level,
        msg: String,
        throwable: Throwable
    ) {
        when (level) {
            Logger.Level.ALL -> Log.i(TAG, msg, throwable)
            Logger.Level.TRACE -> Log.wtf(TAG, msg, throwable)
            Logger.Level.DEBUG -> Log.d(TAG, msg, throwable)
            Logger.Level.INFO -> Log.i(TAG, msg, throwable)
            Logger.Level.WARNING -> Log.w(TAG, msg, throwable)
            Logger.Level.ERROR -> Log.e(TAG, msg, throwable)
            Logger.Level.OFF -> Log.v(TAG, msg, throwable)
        }
    }

    override fun isLoggable(level: Logger.Level): Boolean {
        return Log.isLoggable(
            TAG, when (level) {
                Logger.Level.ALL -> Log.INFO
                Logger.Level.TRACE -> Log.ERROR
                Logger.Level.DEBUG -> Log.DEBUG
                Logger.Level.INFO -> Log.INFO
                Logger.Level.WARNING -> Log.WARN
                Logger.Level.ERROR -> Log.ERROR
                Logger.Level.OFF -> Log.VERBOSE
            }
        )
    }
}
