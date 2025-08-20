package com.klyx.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.Process
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.io.bufferedReader
import kotlin.io.copyTo
import kotlin.io.inputStream
import kotlin.io.lineSequence
import kotlin.io.use
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ProcessResult(
    val exitCode: Int,
    val output: String = "",
    val error: String = "",
    val success: Boolean = exitCode == 0,
    val duration: Long = 0,
    val process: Process? = null
) {
    fun throwOnError(message: String? = null): ProcessResult {
        if (!success) {
            throw ProcessExecutionException(
                message ?: "Process failed with exit code $exitCode",
                this
            )
        }
        return this
    }
}

/**
 * Wrapper for a running process that provides access to streams
 */
class ManagedProcess(
    val process: Process,
    private val builder: ProcessBuilder
) {
    val inputStream: InputStream get() = process.inputStream
    val outputStream: OutputStream get() = process.outputStream
    val errorStream: InputStream get() = process.errorStream

    val isAlive get() = process.isAlive
    val pid: Int
        get() {
            val pid = process::class.java.getDeclaredField("pid").apply { isAccessible = true }
            return pid.get(process) as? Int ?: 0
        }

    suspend fun waitFor(): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val exitCode = process.waitFor()
        val duration = System.currentTimeMillis() - startTime

        ProcessResult(
            exitCode = exitCode,
            success = exitCode == 0,
            duration = duration,
            process = process
        )
    }

    suspend fun waitFor(timeout: Duration): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val finished = process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        val duration = System.currentTimeMillis() - startTime

        val exitCode = if (finished) process.exitValue() else -1

        ProcessResult(
            exitCode = exitCode,
            success = finished && exitCode == 0,
            duration = duration,
            process = process
        )
    }

    fun destroy() {
        process.destroy()
    }

    fun destroyForcibly(): Process {
        return process.destroyForcibly()
    }
}

class ProcessExecutionException(
    message: String,
    val result: ProcessResult
) : Exception("$message\nExit code: ${result.exitCode}\nError output: ${result.error}")

@DslMarker
private annotation class ProcessDsl

@ProcessDsl
class ProcessBuilder @PublishedApi internal constructor(
    private val command: String
) {
    private val args = mutableListOf<String>()
    private var workingDir: File? = null
    private val envVars = mutableMapOf<String, String>()
    private var inputText: String? = null
    private var inputFile: File? = null
    private var inputStream: InputStream? = null
    private var outputFile: File? = null
    private var errorFile: File? = null
    private var appendOutput = false
    private var appendError = false
    private var timeout: Duration? = null
    private var inheritIO = false
    private var captureOutput = true
    private var captureError = true
    private var onOutputLine: ((String) -> Unit)? = null
    private var onErrorLine: ((String) -> Unit)? = null
    private var destroyOnTimeout = true

    constructor(vararg commands: String) : this(commands.first()) {
        args.addAll(commands.drop(1))
    }

    // Command arguments
    fun args(vararg arguments: String) = apply {
        args.addAll(arguments)
    }

    fun args(arguments: List<String>) = apply {
        args.addAll(arguments)
    }

    fun arg(argument: String) = apply {
        args.add(argument)
    }

    // Working directory
    fun workingDirectory(path: String) = apply {
        workingDir = File(path)
    }

    fun workingDirectory(path: Path) = apply {
        workingDir = path.toFile()
    }

    fun workingDirectory(file: File) = apply {
        workingDir = file
    }

    // Environment variables
    fun env(key: String, value: String) = apply {
        envVars[key] = value
    }

    fun env(variables: Map<String, String>) = apply {
        envVars.putAll(variables)
    }

    fun env(block: MutableMap<String, String>.() -> Unit) = apply {
        envVars.apply(block)
    }

    // Input handling
    fun input(text: String) = apply {
        inputText = text
    }

    fun input(file: File) = apply {
        inputFile = file
    }

    fun input(stream: InputStream) = apply {
        inputStream = stream
    }

    // Output redirection
    fun output(file: File, append: Boolean = false) = apply {
        outputFile = file
        appendOutput = append
    }

    fun output(file: String, append: Boolean = false) = apply {
        outputFile = File(file)
        appendOutput = append
    }

    fun appendOutput(file: File) = output(file, append = true)
    fun appendOutput(file: String) = output(file, append = true)

    // Error redirection
    fun error(file: File, append: Boolean = false) = apply {
        errorFile = file
        appendError = append
    }

    fun error(file: String, append: Boolean = false) = apply {
        errorFile = File(file)
        appendError = append
    }

    fun appendError(file: File) = error(file, append = true)
    fun appendError(file: String) = error(file, append = true)

    // Redirect error to output
    fun redirectErrorToOutput() = apply {
        errorFile = outputFile
        appendError = appendOutput
    }

    // Timeout
    fun timeout(duration: Duration, destroyOnTimeout: Boolean = true) = apply {
        this.timeout = duration
        this.destroyOnTimeout = destroyOnTimeout
    }

    fun timeout(seconds: Long, destroyOnTimeout: Boolean = true) =
        timeout(seconds.seconds, destroyOnTimeout)

    // IO options
    fun inheritIO() = apply {
        inheritIO = true
        captureOutput = false
        captureError = false
    }

    fun captureOutput(capture: Boolean = true) = apply {
        captureOutput = capture
    }

    fun captureError(capture: Boolean = true) = apply {
        captureError = capture
    }

    // Real-time output callbacks
    fun onOutput(callback: (String) -> Unit) = apply {
        onOutputLine = callback
    }

    fun onError(callback: (String) -> Unit) = apply {
        onErrorLine = callback
    }

    suspend fun start(): ManagedProcess = withContext(Dispatchers.IO) {
        val processBuilder = buildProcessBuilder()
        val process = processBuilder.start()

        launch {
            process.outputStream.use { output ->
                when {
                    inputText != null -> output.write(inputText!!.toByteArray())
                    inputFile != null -> inputFile!!.inputStream().use { it.copyTo(output) }
                    inputStream != null -> inputStream!!.use { it.copyTo(output) }
                }
            }
        }

        val outputDeferred = async {
            if (captureOutput && outputFile == null) {
                captureStream(process.inputStream, onOutputLine)
            } else ""
        }

        val errorDeferred = async {
            if (captureError && errorFile == null) {
                captureStream(process.errorStream, onErrorLine)
            } else ""
        }

        ManagedProcess(
            process,
            this@ProcessBuilder
        ).also { outputDeferred.await();errorDeferred.await() }
    }

    fun startBlocking(): ManagedProcess = runBlocking { start() }

    suspend fun execute() = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        val processBuilder = buildProcessBuilder()
        val process = processBuilder.start()

        launch {
            process.outputStream.use { output ->
                when {
                    inputText != null -> output.write(inputText!!.toByteArray())
                    inputFile != null -> inputFile!!.inputStream().use { it.copyTo(output) }
                    inputStream != null -> inputStream!!.use { it.copyTo(output) }
                }
            }
        }

        val outputDeferred = async {
            if (captureOutput && outputFile == null) {
                captureStream(process.inputStream, onOutputLine)
            } else ""
        }

        val errorDeferred = async {
            if (captureError && errorFile == null) {
                captureStream(process.errorStream, onErrorLine)
            } else ""
        }

        val exitCode = if (timeout != null) {
            if (process.waitFor(timeout!!.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                process.exitValue()
            } else {
                if (destroyOnTimeout) {
                    process.destroyForcibly()
                }
                -1 // Timeout exit code
            }
        } else {
            process.waitFor()
        }

        val output = outputDeferred.await()
        val error = errorDeferred.await()
        val duration = System.currentTimeMillis() - startTime

        ProcessResult(exitCode, output, error, duration = duration, process = process)
    }

    fun executeBlocking() = runBlocking { execute() }

    internal fun buildProcessBuilder(): java.lang.ProcessBuilder {
        return java.lang.ProcessBuilder().apply {
            command(buildCommand())

            workingDir?.let { directory(it) }

            if (envVars.isNotEmpty()) {
                environment().putAll(envVars)
            }

            if (inheritIO) {
                inheritIO()
            } else {
                if (outputFile != null) {
                    redirectOutput(
                        if (appendOutput) java.lang.ProcessBuilder.Redirect.appendTo(outputFile)
                        else java.lang.ProcessBuilder.Redirect.to(outputFile)
                    )
                }

                if (errorFile != null) {
                    redirectError(
                        if (appendError) java.lang.ProcessBuilder.Redirect.appendTo(errorFile)
                        else java.lang.ProcessBuilder.Redirect.to(errorFile)
                    )
                }
            }
        }
    }

    internal fun buildCommand(): List<String> {
        return listOf(command) + args
    }

    private suspend fun captureStream(
        inputStream: InputStream,
        lineCallback: ((String) -> Unit)?
    ): String = withContext(Dispatchers.IO) {
        val output = StringBuilder()
        inputStream.bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                output.appendLine(line)
                lineCallback?.invoke(line)
            }
        }
        output.toString().trimEnd()
    }
}

val ProcessBuilder.commands get() = buildCommand()
fun ProcessBuilder.asJavaProcessBuilder() = buildProcessBuilder()

@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
inline fun process(
    command: String,
    @BuilderInference
    block: ProcessBuilder.() -> Unit = {}
): ProcessBuilder {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return ProcessBuilder(command).apply(block)
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
@Suppress("SpreadOperator")
inline fun process(
    commands: Array<String>,
    @BuilderInference
    block: ProcessBuilder.() -> Unit = {}
): ProcessBuilder {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return ProcessBuilder(*commands).apply(block)
}

suspend fun String.execute() = process(this).execute()
fun String.executeBlocking() = process(this).executeBlocking()

suspend fun String.start() = process(this).start()
fun String.startBlocking() = process(this).startBlocking()

suspend fun List<String>.execute() = process(toTypedArray()).execute()
fun Collection<String>.executeBlocking() = process(toTypedArray()).executeBlocking()

suspend fun List<String>.start() = process(toTypedArray()).start()
fun Collection<String>.startBlocking() = process(toTypedArray()).startBlocking()

infix fun ProcessBuilder.pipe(other: ProcessBuilder): PipeBuilder {
    return PipeBuilder(listOf(this, other))
}

// Piping (conceptual - would need more complex implementation)
class PipeBuilder(
    private val processes: List<ProcessBuilder>
) {
    suspend fun execute(): ProcessResult {
        var currentOutput = ""
        var lastResult: ProcessResult? = null

        for ((index, process) in processes.withIndex()) {
            val processToExecute = if (index == 0) {
                process
            } else {
                // Pass previous output as input to current process
                process.apply { input(currentOutput) }
            }

            val result = processToExecute.captureOutput(true).execute()
            lastResult = result

            if (!result.success) {
                return result // Return failed result immediately
            }

            currentOutput = result.output
        }

        return lastResult!!
    }

    fun executeBlocking() = runBlocking { execute() }

    infix fun pipe(other: ProcessBuilder): PipeBuilder {
        return PipeBuilder(processes + other)
    }
}
