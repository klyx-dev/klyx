package com.klyx

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

class RustLanguageServerService : Service() {
    companion object {
        private const val TAG = "RustLanguageServer"
    }

    private var rustAnalyzerProcess: Process? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread {
            val port = intent?.getIntExtra("port", 0) ?: 0
            val projectPath = intent?.getStringExtra("projectPath") ?: ""

            val socket = ServerSocket(port)
            Log.d(TAG, "Starting Rust Language Server socket on port ${socket.localPort}")

            val socketClient = socket.accept()
            Log.d(TAG, "Connected to client on port ${socketClient.port}")

            runCatching {
                startRustAnalyzer(
                    projectPath,
                    socketClient.getInputStream(),
                    socketClient.getOutputStream()
                )
            }.onFailure { exception ->
                Log.e(TAG, "Failed to start Rust Analyzer", exception)
            }

            socketClient.close()
            socket.close()
        }
        return START_STICKY
    }

    private fun startRustAnalyzer(
        projectPath: String,
        inputStream: InputStream,
        outputStream: OutputStream
    ) {
        try {
            // Method 1: Use rust-analyzer binary directly (if available)
            val rustAnalyzerPath = findRustAnalyzer()

            if (rustAnalyzerPath != null) {
                Log.d(TAG, "Found rust-analyzer at: $rustAnalyzerPath")
                val file = File(rustAnalyzerPath)
                println("Is Executable: ${file.canExecute()}")
                startRustAnalyzerProcess(rustAnalyzerPath, projectPath, inputStream, outputStream)
            } else {
                Log.e(TAG, "rust-analyzer not found. Please install rust-analyzer.")
                downloadAndExtractRustAnalyzer()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting rust-analyzer", e)
            throw e
        }
    }

    private fun startRustAnalyzerProcess(
        rustAnalyzerPath: String,
        projectPath: String,
        inputStream: InputStream,
        outputStream: OutputStream
    ) {
        try {
            val processBuilder = ProcessBuilder("/system/bin/linker64", rustAnalyzerPath)
                .redirectErrorStream(true)
            processBuilder.directory(File(projectPath))

            // Set environment variables
            val env = processBuilder.environment()
            env["RUST_LOG"] = "rust_analyzer=info"

            rustAnalyzerProcess = processBuilder.start()

            val processInputStream = rustAnalyzerProcess!!.inputStream
            val processOutputStream = rustAnalyzerProcess!!.outputStream

            // Create LSP launcher
            val launcher = LSPLauncher.createClientLauncher(
                SimpleLanguageClient(),
                inputStream,
                outputStream
            )

            val server = launcher.remoteProxy

            // Start listening for LSP messages
            val listening = launcher.startListening()

            // Bridge the streams between socket and process
            serviceScope.launch {
                try {
                    inputStream.copyTo(processOutputStream)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream closed", e)
                }
            }

            serviceScope.launch {
                try {
                    processInputStream.copyTo(outputStream)
                } catch (e: IOException) {
                    Log.d(TAG, "Output stream closed", e)
                }
            }

            listening.get()

        } catch (e: Exception) {
            Log.e(TAG, "Error in rust-analyzer process", e)
        }
    }

    private fun findRustAnalyzer(): String? {
        val possiblePaths = listOf(
            "/system/bin/rust-analyzer",
            "/data/local/tmp/rust-analyzer",
            "${applicationContext.filesDir}/rust-analyzer",
            "${applicationContext.cacheDir}/rust-analyzer"
        )

        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists() && file.canExecute()) {
                return path
            }
        }

        // Try to find in PATH
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "rust-analyzer"))
            process.waitFor()
            if (process.exitValue() == 0) {
                val path = process.inputStream.bufferedReader().readLine()
                if (path.isNotEmpty()) {
                    return path.trim()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not find rust-analyzer in PATH", e)
        }

        return null
    }

    private fun downloadAndExtractRustAnalyzer() {
        // Implementation to download rust-analyzer binary
        // This is a placeholder - you would need to implement downloading
        // the appropriate rust-analyzer binary for Android architecture
        Log.d(TAG, "Download and extract rust-analyzer functionality needed")

        // You could download from: https://github.com/rust-lang/rust-analyzer/releases
        // and extract to applicationContext.filesDir or cacheDir

        val assetName = "rust-analyzer"
        val outName = "rust-analyzer"

        val input = assets.open(assetName)
        val outFile = File(applicationContext.filesDir, outName)

        FileOutputStream(outFile).use { input.copyTo(it) }
        outFile.setExecutable(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        rustAnalyzerProcess?.destroy()
        Log.d(TAG, "Rust Language Server Service destroyed")
    }

    // Simple Language Client implementation
    private class SimpleLanguageClient : LanguageClient {
        override fun telemetryEvent(obj: Any?) {
            Log.d(TAG, "Telemetry: $obj")
        }

        override fun publishDiagnostics(params: PublishDiagnosticsParams?) {
            Log.d(TAG, "Diagnostics: ${params?.diagnostics?.size} issues")
        }

        override fun showMessage(params: MessageParams?) {
            Log.d(TAG, "Message: ${params?.message}")
        }

        override fun showMessageRequest(params: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
            Log.d(TAG, "Message Request: ${params?.message}")
            return CompletableFuture.completedFuture(null)
        }

        override fun logMessage(params: MessageParams?) {
            Log.d(TAG, "Log: ${params?.message}")
        }
    }
}
