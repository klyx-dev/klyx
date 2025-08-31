package com.klyx.editor.lsp

import android.content.Context
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.onSuccess
import com.klyx.core.asJavaProcessBuilder
import com.klyx.core.logging.logger
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.extension.LocalExtension
import com.klyx.extension.api.SystemWorktree
import com.klyx.terminal.ubuntuProcess
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

@OptIn(ExperimentalCodeEditorApi::class)
internal fun CodeEditorState.createLanguageServerDefinition(
    context: Context,
    extension: LocalExtension,
    languageServerId: String,
    languageId: String
): LanguageServerDefinition {
    val fileExtension = file.extension

    return object : LanguageServerDefinition() {
        init {
            ext = fileExtension
            languageIds = mapOf(fileExtension to languageId)
        }

        override fun createConnectionProvider(workingDir: String): StreamConnectionProvider {
            return ProcessStreamConnectionProvider(context, languageServerId, extension, workingDir)
        }

        override fun getInitializationOptions(uri: URI?): Any? = runBlocking {
            val options = extension.languageServerInitializationOptions(
                languageServerId, SystemWorktree
            ).fold(
                success = { it.getOrNull() },
                failure = { null }
            ) ?: return@runBlocking null

            val jsonElement = Json.parseToJsonElement(options)
            if (jsonElement is JsonObject) jsonElement.toMap() else null
        }
    }
}

private class ProcessStreamConnectionProvider(
    private val context: Context,
    private val languageServerId: String,
    private val extension: LocalExtension,
    private val workingDir: String
) : StreamConnectionProvider {

    private val logger = logger(languageServerId)
    private lateinit var process: Process

    override val inputStream: InputStream get() = process.inputStream
    override val outputStream: OutputStream get() = process.outputStream

    override fun start(): Unit = runBlocking {
        extension.languageServerCommand(languageServerId, SystemWorktree).onSuccess { (command, args, env) ->
            logger.info {
                "starting language server process. binary path: $command, working directory: $workingDir, args: $args"
            }

            with(context) {
                val pb = ubuntuProcess(command, *args.toTypedArray()) {
                    env { putAll(env) }
                    workingDirectory(workingDir)
                }

                process = pb.asJavaProcessBuilder().start()
            }
        }
    }

    override fun close() {
        if (::process.isInitialized) process.destroy()
    }
}
