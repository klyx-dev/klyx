package com.klyx

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.klyx.terminal.klyxBinDir
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.lsp.client.connection.SocketStreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.text.ContentIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.ServerSocket
import java.util.zip.ZipFile

class RustLspActivity : BaseEditorActivity() {
    private lateinit var lspEditor: LspEditor
    private lateinit var lspProject: LspProject

    private lateinit var rootMenu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "LSP Test - Rust"

        ensureTextmateTheme()

        lifecycleScope.launch {
            unAssets()
            connectToLanguageServer()
            setEditorText()
        }
    }

    private suspend fun setEditorText() {
        val text = withContext(Dispatchers.IO) {
            ContentIO.createFrom(
                filesDir?.resolve("rustProject/src/main.rs")!!.inputStream()
            )
        }
        editor.setText(text, null)
    }

    private suspend fun unAssets() = withContext(Dispatchers.IO) {
        // Extract Rust project files from assets
        val zipFile = ZipFile(packageResourcePath)
        val zipEntries = zipFile.entries()
        while (zipEntries.hasMoreElements()) {
            val zipEntry = zipEntries.nextElement()
            val fileName = zipEntry.name
            if (fileName.startsWith("assets/rustProject/")) {
                val inputStream = zipFile.getInputStream(zipEntry)
                val filePath = filesDir?.resolve(fileName.substring("assets/".length))
                filePath?.parentFile?.mkdirs()
                val outputStream = FileOutputStream(filePath)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            }
        }
        zipFile.close()
    }

    private suspend fun connectToLanguageServer() = withContext(Dispatchers.IO) {

        withContext(Dispatchers.Main) {
            toast("(Rust Activity) Starting Rust Analyzer...")
            editor.editable = false
        }

        val port = randomPort()
        val projectPath = filesDir?.resolve("rustProject")?.absolutePath ?: ""

//        startService(
//            Intent(this@RustLspActivity, RustLanguageServerService::class.java).apply {
//                putExtra("port", port)
//                putExtra("projectPath", projectPath)
//            }
//        )

        val rustServerDefinition = object : CustomLanguageServerDefinition(
            "rs",
            ServerConnectProvider { workingDir ->
                object : StreamConnectionProvider {
                    lateinit var process: Process

                    override val inputStream: InputStream
                        get() = process.inputStream

                    override val outputStream: OutputStream
                        get() = process.outputStream

                    override fun start() {
                        val execPath = File(klyxBinDir, "rust-analyzer").absolutePath

                        println(workingDir)
                        val processBuilder = ProcessBuilder(execPath)
                            .directory(File(workingDir))
                            .redirectOutput(File(getExternalFilesDir(null), "ra.log"))
                            .redirectErrorStream(true)
                            .apply {
                                environment()["RA_LOG"] = "info"
                            }

                        process = processBuilder.start()
                    }

                    override fun close() {
                        if (::process.isInitialized) {
                            process.destroy()
                        }
                    }
                }
            }
        ) {
            private val _eventListener = EventListener(this@RustLspActivity)
            override val eventListener: EventHandler.EventListener get() = _eventListener
        }

        lspProject = LspProject(projectPath)
        lspProject.addServerDefinition(rustServerDefinition)

        withContext(Dispatchers.Main) {
            lspEditor = lspProject.createEditor("$projectPath/src/main.rs")
            val wrapperLanguage = createTextMateLanguage()
            lspEditor.wrapperLanguage = wrapperLanguage
            lspEditor.editor = editor
        }

        var connected: Boolean

        try {
            lspEditor.connectWithTimeout()

            // Set up workspace folders for Rust project
            lspEditor.requestManager?.didChangeWorkspaceFolders(
                DidChangeWorkspaceFoldersParams().apply {
                    this.event = WorkspaceFoldersChangeEvent().apply {
                        added = listOf(
                            WorkspaceFolder("file://$projectPath", "RustProject")
                        )
                    }
                }
            )

            connected = true
        } catch (e: Exception) {
            connected = false
            //e.printStackTrace()
            Log.e("RustLsp", "Unable to connect to Rust Analyzer")
        }

        lifecycleScope.launch(Dispatchers.Main) {
            if (connected) {
                toast("Rust Analyzer initialized successfully")
            } else {
                toast("Unable to connect to Rust Analyzer")
            }
            editor.editable = true
        }
    }

    private fun toast(text: String) {
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun randomPort(): Int {
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort
        serverSocket.close()
        return port
    }

    private fun createTextMateLanguage(): TextMateLanguage {
        GrammarRegistry.getInstance().loadGrammars(
            languages {
                language("rust") {
                    grammar = "textmate/rust/syntaxes/rust.tmLanguage.json"
                    scopeName = "source.rust"
                    languageConfiguration = "textmate/rust/language-configuration.json"
                }
            }
        )

        return TextMateLanguage.create("source.rust", false)
    }

    private fun ensureTextmateTheme() {
        var editorColorScheme = editor.colorScheme

        if (editorColorScheme is TextMateColorScheme) {
            return
        }

        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(assets)
        )

        val themeRegistry = ThemeRegistry.getInstance()
        val path = "textmate/quietlight.json"

        themeRegistry.loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                ), "quietlight"
            )
        )

        themeRegistry.setTheme("quietlight")
        editorColorScheme = TextMateColorScheme.create(themeRegistry)
        editor.colorScheme = editorColorScheme
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_lsp, menu)
        rootMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.code_format) {
            val cursor = editor.text.cursor
            if (cursor.isSelected) {
                editor.formatCodeAsync(cursor.left(), cursor.right())
            } else {
                editor.formatCodeAsync()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()

        editor.release()
        lifecycleScope.launch {
            lspEditor.dispose()
            lspProject.dispose()
        }
        stopService(Intent(this@RustLspActivity, RustLanguageServerService::class.java))
    }

    class EventListener(
        activity: RustLspActivity
    ) : EventHandler.EventListener {
        private val activityRef = WeakReference(activity)

        override fun initialize(server: LanguageServer?, result: InitializeResult) {
            activityRef.get()?.apply {
                runOnUiThread {
                    rootMenu.findItem(R.id.code_format).isEnabled =
                        result.capabilities.documentFormattingProvider != null
                }
            }
        }
    }
}
