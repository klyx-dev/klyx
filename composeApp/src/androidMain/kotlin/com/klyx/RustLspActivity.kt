package com.klyx

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.klyx.core.asJavaProcessBuilder
import com.klyx.terminal.ubuntuProcess
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.text.ContentIO
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
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
//            unAssets()
            connectToLanguageServer()
//            setEditorText()
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

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun connectToLanguageServer() = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            toast("Starting pylsp...")
            editor.editable = false
        }

        val projectPath = filesDir?.resolve("pythonProject")?.absolutePath.orEmpty()
        val mainFile = File("$projectPath/main.py").apply {
            parentFile?.mkdirs()
            writeText("print('hello from pylsp')")
        }

        val text = withContext(Dispatchers.IO) { ContentIO.createFrom(mainFile.inputStream()) }
        withContext(Dispatchers.Main) {
            editor.setText(text, null)
        }

        val definition = object : LanguageServerDefinition() {
            init {
                this.ext = "py"
                this.languageIds = mapOf("py" to "python")
            }

            override fun createConnectionProvider(workingDir: String): StreamConnectionProvider {
                return object : StreamConnectionProvider {
                    lateinit var process: Process

                    override val inputStream: InputStream
                        get() = process.inputStream

                    override val outputStream: OutputStream
                        get() = process.outputStream

                    override fun start() {
                        val lp = ubuntuProcess("pylsp") {
                            workingDirectory(workingDir)
                        }
                        process = lp.asJavaProcessBuilder().start()
                    }

                    override fun close() {
                        if (::process.isInitialized) {
                            process.destroy()
                        }
                    }
                }
            }
        }

        lspProject = LspProject(projectPath)
        lspProject.addServerDefinition(definition)

        withContext(Dispatchers.Main) {
            lspEditor = lspProject.createEditor("$projectPath/main.py")
            lspEditor.editor = editor
        }

        var connected: Boolean
        try {
            lspEditor.connectWithTimeout()

            val fileUri = "file://$projectPath/main.py"
            val fileContent = mainFile.readText()

//            val didOpenParams = DidOpenTextDocumentParams(
//                TextDocumentItem(fileUri, definition.languageIdFor("py"), 1, fileContent)
//            )
//            lspEditor.requestManager?.didOpen(didOpenParams)
            lspEditor.openDocument()

            connected = true
        } catch (e: Exception) {
            connected = false
            Log.e("PythonLsp", "Unable to connect to pylsp", e)
        }

        lifecycleScope.launch(Dispatchers.Main) {
            if (connected) {
                toast("pylsp initialized successfully ✅")
            } else {
                toast("Unable to connect to pylsp ❌")
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

    class El() : io.github.rosemoe.sora.lsp.events.EventListener {
        override val eventName: String
            get() = "textDocument/didChange"

        override fun handle(context: EventContext) {
            TODO("Not yet implemented")
        }
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

        override fun onShowMessage(messageParams: MessageParams?) {
            activityRef.get()?.apply {
                runOnUiThread {
                    toast(messageParams?.message ?: "Unknown message")
                }
            }
        }

        override fun onLogMessage(messageParams: MessageParams?) {
            Log.d("RustLsp", messageParams?.message ?: "Unknown message")
        }
    }
}
