package com.klyx.plugin

import com.klyx.api.InternalKlyxApi
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.file.providerKey
import com.klyx.api.lsp.LanguageServerProvider
import com.klyx.api.lsp.LanguageServerRegistration
import com.klyx.api.lsp.LanguageServerRegistry
import com.klyx.api.plugin.KlyxPlugin
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Single
class LanguageServerRegistryImpl : LanguageServerRegistry {

    private data class RegistrationInfo(
        val id: String,
        val pattern: String,
        val provider: LanguageServerProvider,
        val plugin: KlyxPlugin?
    )

    private val registrations = ConcurrentHashMap<String, RegistrationInfo>()
    private val nextId = AtomicInteger(0)

    context(plugin: KlyxPlugin)
    override fun register(pattern: String, provider: LanguageServerProvider): LanguageServerRegistration {
        return doRegister(pattern, provider, plugin)
    }

    @InternalKlyxApi
    override fun registerInternal(pattern: String, provider: LanguageServerProvider): LanguageServerRegistration {
        return doRegister(pattern, provider, null)
    }

    private fun doRegister(
        pattern: String,
        provider: LanguageServerProvider,
        plugin: KlyxPlugin?
    ): LanguageServerRegistration {
        val id = nextId.getAndIncrement().toString()
        val info = RegistrationInfo(id, pattern, provider, plugin)
        registrations[id] = info
        return object : LanguageServerRegistration {
            override fun unregister() {
                this@LanguageServerRegistryImpl.unregister(id)
            }
        }
    }

    override fun unregister(id: String) {
        registrations.remove(id)
    }

    override fun getProviders(file: KxFile): List<LanguageServerRegistry.RegisteredProvider> {
        val key = file.providerKey
        return registrations.values
            .filter { it.pattern == key }
            .sortedBy { it.id.toInt() }
            .map { LanguageServerRegistry.RegisteredProvider(it.id, it.provider) }
    }
}
