package com.klyx.compiler.plugin

import com.klyx.compiler.plugin.fir.KlyxFirExtensionRegistrar
import com.klyx.compiler.plugin.ir.KlyxIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class KlyxCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId = BuildConfig.KOTLIN_PLUGIN_ID
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val descriptorOutputDir = configuration[KlyxConfigurationKeys.DESCRIPTOR_OUTPUT_DIR]
        val descriptorIcon = configuration[KlyxConfigurationKeys.DESCRIPTOR_ICON]
        val messageCollector = configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY]
            ?: MessageCollector.NONE

        FirExtensionRegistrarAdapter.registerExtension(KlyxFirExtensionRegistrar(messageCollector))
        IrGenerationExtension.registerExtension(
            KlyxIrGenerationExtension(
                descriptorOutputDir = descriptorOutputDir,
                descriptorIcon = descriptorIcon,
                messageCollector = messageCollector
            )
        )
    }
}
