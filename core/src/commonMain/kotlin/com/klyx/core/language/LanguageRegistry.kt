package com.klyx.core.language

import com.klyx.core.io.join
import com.klyx.core.logging.logger
import com.klyx.core.lsp.LanguageServerName
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.io.files.Path

private data class LanguageRegistryState(
    val lspAdapters: HashMap<LanguageName, MutableList<CachedLspAdapter>> = hashMapOf(),
    val allLspAdapters: HashMap<LanguageServerName, CachedLspAdapter> = hashMapOf()
)

class LanguageRegistry private constructor(
    private val state: LanguageRegistryState = LanguageRegistryState(),
    private var languageServerDownloadDir: Path? = null
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

            state.allLspAdapters[name] = CachedLspAdapter(adapter)
        }
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

    fun lspAdapters(languageName: LanguageName) = state.lspAdapters[languageName] ?: emptyList()
    fun allLspAdapters() = state.allLspAdapters.values.toList()
    fun adapterForName(name: LanguageServerName) = state.allLspAdapters[name]

    fun setLanguageServerDownloadDir(dir: Path) {
        languageServerDownloadDir = dir
    }

    fun languageServerDownloadDir(name: LanguageServerName) = languageServerDownloadDir?.join(name)
}
