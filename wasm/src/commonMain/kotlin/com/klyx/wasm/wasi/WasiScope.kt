package com.klyx.wasm.wasi

import at.released.weh.bindings.chasm.wasip1.ChasmWasiPreview1Builder
import at.released.weh.filesystem.stdio.StdioSink
import at.released.weh.filesystem.stdio.StdioSource
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.embedding.shapes.Store

@DslMarker
internal annotation class WasiDsl

@WasiDsl
@ExperimentalWasiApi
class WasiScope @PublishedApi internal constructor(
    private val store: Store
) {
    private var arguments = listOf<String>()
    private var environment = mapOf<String, String>()
    private var directories = mapOf<String, String>()

    private var stdin: StdioSource? = null
    private var stdout: StdioSink? = null
    private var stderr: StdioSink? = null

    private var workingDir: String? = null

    init {
        env("RUST_BACKTRACE", "full")
    }

    fun inheritSystem() = apply {
        stdin = null
        stdout = null
        stderr = null
    }

    fun stdin(source: StdioSourceProvider) = apply {
        this.stdin = source.open()
    }

    fun stdout(sink: StdioSinkProvider) = apply {
        this.stdout = sink.open()
    }

    fun stderr(sink: StdioSinkProvider) = apply {
        this.stderr = sink.open()
    }

    fun workingDirectory(directory: String) = apply {
        this.workingDir = directory

        // TODO: remove?
        env("PWD", directory)
    }

    fun arguments(vararg arguments: String) = apply { this.arguments = arguments.toList() }
    fun environment(environment: Map<String, String>) = apply { this.environment = environment }
    fun directories(directories: Map<String, String>) = apply { this.directories = directories }

    fun directory(system: String, wasi: String) = apply {
        directories += system to wasi
    }

    fun env(name: String, value: String) = apply { environment += name to value }

    fun env(builderAction: MutableMap<String, String>.() -> Unit) = apply {
        environment += buildMap(builderAction)
    }

    @PublishedApi
    internal fun build() = ChasmWasiPreview1Builder(store) {
        host = EmbedderHost {
            setCommandArgs { arguments }
            setSystemEnv { environment }

            with(this@WasiScope) {
                stdin?.let { setStdin { it } }
                stdout?.let { setStdout { it } }
                stderr?.let { setStderr { it } }
            }

            fileSystem {
                directories.forEach { (system, wasi) ->
                    addPreopenedDirectory(system, wasi)
                }

                setCurrentWorkingDirectory(workingDir)
            }
        }
    }.build()
}
