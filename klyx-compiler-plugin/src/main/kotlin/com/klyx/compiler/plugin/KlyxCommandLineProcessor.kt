package com.klyx.compiler.plugin

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

class KlyxCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = BuildConfig.KOTLIN_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = KlyxPluginIds.OPTION_DESCRIPTOR_OUTPUT_DIR,
            valueDescription = "<path>",
            description = "Directory to write the generated plugin.json descriptor into.",
            required = false
        ),
        CliOption(
            optionName = KlyxPluginIds.OPTION_DESCRIPTOR_ICON,
            valueDescription = "<name>",
            description = "Bundle-root icon filename to write into the generated plugin.json descriptor.",
            required = false
        )
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            KlyxPluginIds.OPTION_DESCRIPTOR_OUTPUT_DIR -> {
                configuration.put(KlyxConfigurationKeys.DESCRIPTOR_OUTPUT_DIR, value)
            }

            KlyxPluginIds.OPTION_DESCRIPTOR_ICON -> {
                configuration.put(KlyxConfigurationKeys.DESCRIPTOR_ICON, value)
            }

            else -> error("Unexpected config option: '${option.optionName}'")
        }
    }
}

object KlyxConfigurationKeys {
    val DESCRIPTOR_OUTPUT_DIR: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("directory to write generated plugin.json into")

    val DESCRIPTOR_ICON: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("bundle-root icon filename to write into generated plugin.json")
}
