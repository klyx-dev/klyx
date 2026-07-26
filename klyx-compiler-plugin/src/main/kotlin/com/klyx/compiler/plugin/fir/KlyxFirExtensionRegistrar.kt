package com.klyx.compiler.plugin.fir

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class KlyxFirExtensionRegistrar(private val messageCollector: MessageCollector) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +::KlyxManifestCheckers
        +{ session: FirSession -> KlyxDescriptorGenerationExtension(session, messageCollector) }
    }
}
