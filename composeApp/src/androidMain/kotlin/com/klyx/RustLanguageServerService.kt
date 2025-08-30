package com.klyx

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.klyx.terminal.internal.downloadPackage
import com.klyx.terminal.internal.extractTarGz
import com.klyx.terminal.klyxFilesDir
import com.klyx.terminal.localProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread {
            val port = intent?.getIntExtra("port", 0) ?: 0
            val projectPath = intent?.getStringExtra("projectPath").orEmpty()

            val socket = ServerSocket(port)
            Log.d(TAG, "Starting Rust Language Server socket on port ${socket.localPort}")

            val socketClient = socket.accept()
            Log.d(TAG, "Connected to client on port ${socketClient.port}")

            runCatching {
                runBlocking {
                    downloadPackage("rust-analyzer") {
                        extractTarGz(it.absolutePath, klyxFilesDir.absolutePath)
                    }
                }

                val rustAnalyzerProcess = localProcess("rust-analyzer") {
                    env("RUST_LOG", "rust_analyzer=info")
                    workingDirectory(projectPath)
                }.startBlocking()

                runBlocking { rustAnalyzerProcess.waitFor() }

                val processInputStream = rustAnalyzerProcess.inputStream
                val processOutputStream = rustAnalyzerProcess.outputStream

                // Create LSP launcher
                val launcher = LSPLauncher.createClientLauncher(
                    SimpleLanguageClient(),
                    socketClient.inputStream,
                    socketClient.outputStream
                )

                launcher.remoteProxy

                // Start listening for LSP messages
                val listening = launcher.startListening()

                // Bridge the streams between socket and process
                serviceScope.launch {
                    try {
                        socketClient.inputStream.copyTo(processOutputStream)
                    } catch (e: IOException) {
                        Log.d(TAG, "Input stream closed", e)
                    }
                }

                serviceScope.launch {
                    try {
                        processInputStream.copyTo(socketClient.outputStream)
                    } catch (e: IOException) {
                        Log.d(TAG, "Output stream closed", e)
                    }
                }

                listening.get()
            }.onFailure { exception ->
                Log.e(TAG, "Failed to start Rust Analyzer", exception)
            }

            socketClient.close()
            socket.close()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
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
