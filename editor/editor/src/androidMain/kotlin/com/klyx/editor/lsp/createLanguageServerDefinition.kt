package com.klyx.editor.lsp

import android.content.Context
import com.github.michaelbull.result.onSuccess
import com.klyx.core.asJavaProcessBuilder
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.extension.LocalExtension
import com.klyx.extension.api.Worktree
import com.klyx.extension.internal.findBinary
import com.klyx.terminal.ubuntuProcess
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.services.LanguageServer
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

@OptIn(ExperimentalCodeEditorApi::class)
internal fun CodeEditorState.createLanguageServerDefinition(
    context: Context,
    worktree: Worktree,
    extension: LocalExtension,
    languageServerId: String,
    languageId: String
): LanguageServerDefinition {
    val fileExtension = file.extension
    val logger = extension.logger

    return object : LanguageServerDefinition() {
        init {
            ext = fileExtension
            languageIds = mapOf(fileExtension to languageId)
        }

        override fun createConnectionProvider(workingDir: String): StreamConnectionProvider {
            println(findBinary("gradle"))
            return ProcessStreamConnectionProvider(
                context = context,
                languageServerId = languageServerId,
                worktree = worktree,
                extension = extension,
                workingDir = workingDir
            )
        }

        override fun getInitializationOptions(uri: URI?): Any? = runBlocking op@{
            extension.languageServerInitializationOptions(
                languageServerId = languageServerId,
                worktree = worktree
            ).onSuccess {
                it.onSome { options ->
                    if (options.isEmpty()) return@op null

                    val jsonElement = Json.parseToJsonElement(options)
                    return@op if (jsonElement is JsonObject) jsonElement.toMap() else null
                }
            }

            null
        }

        override val eventListener: EventHandler.EventListener
            get() = object : EventHandler.EventListener {
                override fun onLogMessage(messageParams: MessageParams?) {
                    logger.info { "onLogMessage: ${messageParams?.message}" }
                }

                override fun onShowMessage(messageParams: MessageParams?) {
                    logger.info { "onShowMessage: ${messageParams?.message}" }
                }

                override fun initialize(server: LanguageServer?, result: InitializeResult) {
                    logger.debug { "initialize: $result" }
                }
            }
    }
}

private class ProcessStreamConnectionProvider(
    private val context: Context,
    private val languageServerId: String,
    private val worktree: Worktree,
    private val extension: LocalExtension,
    private val workingDir: String
) : StreamConnectionProvider {

    private val logger = extension.logger
    private lateinit var process: Process

    override val inputStream: InputStream get() = process.inputStream
    override val outputStream: OutputStream get() = process.outputStream

    override fun start(): Unit = runBlocking {
        extension.languageServerCommand(languageServerId, worktree).onSuccess { (command, args, env) ->
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
