package com.klyx.editor.language

import com.klyx.core.app.App
import com.klyx.core.file.toOkioPath
import com.klyx.core.io.Paths
import com.klyx.core.io.languagesDir
import com.klyx.core.io.okioFs
import com.klyx.core.logging.logger
import com.klyx.core.lsp.LanguageServerName
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path

private class LanguageRegistryState(
    val lspAdapters: MutableMap<LanguageName, MutableList<CachedLspAdapter>> = mutableMapOf(),
    val allLspAdapters: MutableMap<LanguageServerName, CachedLspAdapter> = mutableMapOf(),
    val availableLspAdapters: MutableMap<LanguageServerName, () -> CachedLspAdapter> = mutableMapOf()
)

class LanguageRegistry private constructor(
    private val state: LanguageRegistryState = LanguageRegistryState(),
    private var languageServerDownloadDir: Path? = Paths.languagesDir.toOkioPath()
) {
    private val lock = reentrantLock()
    private val log = logger()

    companion object {
        val INSTANCE by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { LanguageRegistry() }
    }

    fun registerAvailableLspAdapter(name: LanguageServerName, adapter: LspAdapter) {
        lock.withLock {
            if (adapter.isExtension && state.allLspAdapters[name]?.adapter?.isExtension == false) {
                log.warn {
                    "not registering extension-provided language server $name, since a builtin language server exists with that name"
                }
                return
            }

            state.availableLspAdapters[name] = { CachedLspAdapter(adapter) }
        }
    }

    /**
     * Loads the language server adapter for the language server with the given name.
     */
    fun loadAvailableLspAdapter(name: LanguageServerName) = lock.withLock {
        state.availableLspAdapters[name]?.invoke()
    }

    /**
     * Checks if a language server adapter with the given name is available to be loaded.
     */
    fun isLspAdapterAvailable(name: LanguageServerName) = lock.withLock {
        state.availableLspAdapters.containsKey(name)
    }

    fun registerLspAdapter(languageName: LanguageName, adapter: LspAdapter) {
        lock.withLock {
            if (adapter.isExtension && state.allLspAdapters[adapter.name()]?.adapter?.isExtension == false) {
                log.warn {
                    "not registering extension-provided language server ${adapter.name()} for language $languageName, since a builtin language server exists with that name"
                }
                return
            }

            val cached = CachedLspAdapter(adapter)
            state.lspAdapters.getOrPut(languageName) { mutableListOf() }.add(cached)
            state.allLspAdapters[cached.name] = cached
        }
    }

    fun removeLspAdapter(languageName: LanguageName, name: LanguageServerName) {
        lock.withLock {
            state.lspAdapters[languageName]?.let { adapters ->
                adapters.removeAll { it.name == name }
            }
            state.allLspAdapters.remove(name)
            state.availableLspAdapters.remove(name)
        }
    }

    fun lspAdapters(languageName: LanguageName) = state.lspAdapters[languageName] ?: emptyList()
    fun allLspAdapters() = state.allLspAdapters.values.toList()
    fun adapterForName(name: LanguageServerName) = state.allLspAdapters[name]

    fun setLanguageServerDownloadDir(dir: Path) {
        languageServerDownloadDir = dir
    }

    fun languageServerDownloadDir(name: LanguageServerName) = languageServerDownloadDir?.resolve(name)

    suspend fun deleteServerContainer(name: LanguageServerName) {
        languageServerDownloadDir(name)?.let {
            withContext(Dispatchers.IO) {
                okioFs.deleteRecursively(it)
            }
        }
    }
}
