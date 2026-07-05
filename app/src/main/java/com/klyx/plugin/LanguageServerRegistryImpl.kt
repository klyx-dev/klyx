package com.klyx.plugin

import com.klyx.api.InternalKlyxApi
import com.klyx.api.lsp.LanguageServerProvider
import com.klyx.api.lsp.LanguageServerRegistration
import com.klyx.api.lsp.LanguageServerRegistry
import com.klyx.api.plugin.KlyxPlugin
import org.koin.core.annotation.Single
import org.koin.core.annotation.Singleton
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Single
class LanguageServerRegistryImpl : LanguageServerRegistry {

    private data class RegistrationInfo(
        val id: String,
        val extension: String,
        val provider: LanguageServerProvider,
        val plugin: KlyxPlugin?
    )

    private val registrations = ConcurrentHashMap<String, RegistrationInfo>()
    private val nextId = AtomicInteger(0)

    context(plugin: KlyxPlugin)
    override fun register(extension: String, provider: LanguageServerProvider): LanguageServerRegistration {
        return doRegister(extension, provider, plugin)
    }

    @InternalKlyxApi
    override fun registerInternal(extension: String, provider: LanguageServerProvider): LanguageServerRegistration {
        return doRegister(extension, provider, null)
    }

    private fun doRegister(
        extension: String,
        provider: LanguageServerProvider,
        plugin: KlyxPlugin?
    ): LanguageServerRegistration {
        val id = nextId.getAndIncrement().toString()
        val info = RegistrationInfo(id, extension, provider, plugin)
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

    override fun getProvider(extension: String): LanguageServerProvider? {
        return registrations.values
            .filter { it.extension == extension }
            .maxByOrNull { it.id.toInt() }
            ?.provider
    }
}
