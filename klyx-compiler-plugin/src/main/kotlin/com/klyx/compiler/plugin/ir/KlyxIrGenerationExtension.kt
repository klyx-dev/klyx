package com.klyx.compiler.plugin.ir

import com.klyx.compiler.plugin.KlyxPluginIds
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import java.io.File

class KlyxIrGenerationExtension(
    private val descriptorOutputDir: String?,
    private val messageCollector: MessageCollector
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val finder = pluginContext.finderForBuiltins()
        val pluginDescriptorClass = finder.findClass(KlyxPluginIds.PLUGIN_DESCRIPTOR_CLASS_ID) ?: run {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Klyx compiler plugin: could not resolve PluginDescriptor. Is klyx-api on the classpath?"
            )
            return
        }

        val transformers = listOf(KlyxPluginManifestGenerator(pluginContext, pluginDescriptorClass))
        for (transformer in transformers) {
            moduleFragment.acceptChildrenVoid(transformer)
        }
    }

    private inner class KlyxPluginManifestGenerator(
        private val pluginContext: IrPluginContext,
        private val pluginDescriptorClass: IrClassSymbol
    ) : IrVisitorVoid() {

        override fun visitElement(element: IrElement) {
            when (element) {
                is IrDeclaration, is IrFile, is IrModuleFragment -> element.acceptChildrenVoid(this)
                else -> {}
            }
        }

        override fun visitClass(declaration: IrClass) {
            if (declaration.hasAnnotation(KlyxPluginIds.PLUGIN_MANIFEST_FQN)) {
                fillDescriptor(declaration, pluginDescriptorClass, pluginContext)
            }
            super.visitClass(declaration)
        }

        private fun fillDescriptor(
            pluginClass: IrClass,
            descriptorClassSymbol: IrClassSymbol,
            pluginContext: IrPluginContext
        ) {
            val annotation = pluginClass.getAnnotation(KlyxPluginIds.PLUGIN_MANIFEST_FQN) ?: return

            fun arg(name: String): String? =
                annotation.getValueArgument(Name.identifier(name))
                    ?.let { it as? IrConst }
                    ?.value as? String

            val id = arg("id").orEmpty()
            val rawName = arg("name").orEmpty()
            val version = arg("version").orEmpty()
            val minAppVersion = arg("minAppVersion").orEmpty()
            val maxAppVersion = arg("maxAppVersion").orEmpty().ifBlank { null }
            val description = arg("description").orEmpty()
            val icon = arg("icon").orEmpty().ifBlank { null }
            val license = arg("license").orEmpty()
            val entryClass = pluginClass.kotlinFqName.asString()
            val displayName = rawName.ifBlank { id }

            val companion = pluginClass.companionObject() ?: return
            val descriptorProperty = companion.declarations
                .filterIsInstance<IrProperty>()
                .firstOrNull { it.name == KlyxPluginIds.DESCRIPTOR_PROPERTY_NAME }
                ?: return

            val descriptorConstructor = descriptorClassSymbol.constructors.first()
            val constructorCall: IrConstructorCall = IrConstructorCallImpl.fromSymbolOwner(
                type = descriptorClassSymbol.defaultType,
                constructorSymbol = descriptorConstructor
            ).apply {
                context(pluginContext) {
                    arguments[0] = id.asIrString()
                    arguments[1] = version.asIrString()
                    arguments[2] = displayName.asIrString()
                    arguments[3] = minAppVersion.asIrString()
                    arguments[4] = maxAppVersion?.asIrString()
                    arguments[5] = entryClass.asIrString()
                    arguments[6] = description.asIrString()
                    arguments[7] = icon?.asIrString()
                    arguments[9] = license.asIrString()
                }
            }

            descriptorProperty.backingField?.initializer = null
            val getter = descriptorProperty.getter ?: return
            getter.body = pluginContext.irFactory.createExpressionBody(constructorCall)

            descriptorOutputDir?.let { dir ->
                writeDescriptorJson(
                    dir, id, displayName, version, minAppVersion, maxAppVersion,
                    entryClass, description, icon, license
                )
            }
        }

        context(pluginContext: IrPluginContext)
        private fun String.asIrString() =
            IrConstImpl.string(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.stringType,
                this
            )

        private fun writeDescriptorJson(
            outputDir: String,
            id: String,
            name: String,
            version: String,
            minAppVersion: String,
            maxAppVersion: String?,
            entryClass: String,
            description: String,
            icon: String?,
            license: String
        ) {
            val dir = File(outputDir).apply { mkdirs() }
            val json = buildString {
                appendLine("{")
                appendLine("  \"id\": \"$id\",")
                appendLine("  \"name\": \"$name\",")
                appendLine("  \"version\": \"$version\",")
                appendLine("  \"minAppVersion\": \"$minAppVersion\",")
                if (maxAppVersion != null) appendLine("  \"maxAppVersion\": \"$maxAppVersion\",")
                appendLine("  \"entryClass\": \"$entryClass\",")
                appendLine("  \"description\": \"${description.replace("\"", "\\\"")}\",")
                if (icon != null) appendLine("  \"icon\": \"$icon\",")
                appendLine("  \"license\": \"$license\"")
                appendLine("}")
            }
            File(dir, "plugin.json").writeText(json)
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "Klyx compiler plugin: wrote generated plugin.json for '$id' to $outputDir"
            )
        }
    }
}
