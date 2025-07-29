package com.klyx.wasm.wasi

import com.dylibso.chicory.wasi.WasiOptions
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.klyx.nullInputStream
import com.klyx.nullOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.time.Clock
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import kotlin.io.path.Path

@WasiDsl
@ExperimentalWasiApi
class WasiScope @PublishedApi internal constructor() {
    private var random: Random = ThreadLocalRandom.current()
    private var clock = Clock.systemUTC()
    private var stdout = nullOutputStream()
    private var stderr = nullOutputStream()
    private var stdin = nullInputStream()
    private var arguments = listOf<String>()
    internal var environment = mapOf<String, String>()
    internal var directories = mapOf<String, Path>()

    internal val fs = Jimfs.newFileSystem(
        Configuration.unix()
            .toBuilder()
            .setAttributeViews("unix")
            .build()
    )

    init {
        env("RUST_BACKTRACE", "full")
    }

    fun random(random: Random) = apply { this.random = random }
    fun clock(clock: Clock) = apply { this.clock = clock }

    fun stdout(stdout: OutputStream) = apply { this.stdout = stdout }
    fun stderr(stderr: OutputStream) = apply { this.stderr = stderr }
    fun stdin(stdin: InputStream) = apply { this.stdin = stdin }

    fun inheritSystem() = apply {
        this.stdout = System.out
        this.stdin = System.`in`
        this.stderr = System.err
        this.environment += System.getenv()
    }

    fun arguments(vararg arguments: String) = apply { this.arguments = arguments.toList() }
    fun environment(environment: Map<String, String>) = apply { this.environment = environment }
    fun directories(directories: Map<String, Path>) = apply { this.directories = directories }

    @PublishedApi
    internal fun build() = WasiOptions::class.java.getDeclaredConstructor(
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

@WasiDsl
@ExperimentalWasiApi
fun WasiScope.env(name: String, value: String) = apply { environment += name to value }

@WasiDsl
@ExperimentalWasiApi
fun WasiScope.directory(system: String, wasi: String) = apply {
    directories += wasi to Path(system)
}
