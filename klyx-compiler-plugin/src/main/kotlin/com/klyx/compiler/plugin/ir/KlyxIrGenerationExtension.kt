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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File

class KlyxIrGenerationExtension(
    private val descriptorOutputDir: String?,
    private val descriptorIcon: String?,
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

        private val finder = pluginContext.finderForBuiltins()

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

        private fun IrConstructorCall.stringArg(name: String): String = stringArgOrNull(name).orEmpty()

        private fun IrConstructorCall.stringArgOrNull(name: String): String? {
            val index = symbol
                .owner
                .parameters.find { it.name == Name.identifier(name) }
                ?.indexInParameters
                ?: return null
            val expression = arguments[index] ?: symbol.owner.parameters[index].defaultValue?.expression
            return expression?.let { it as? IrConst }?.value as? String
        }

        private fun fillDescriptor(
            pluginClass: IrClass,
            descriptorClassSymbol: IrClassSymbol,
            pluginContext: IrPluginContext
        ) {
            val annotation = pluginClass.getAnnotation(KlyxPluginIds.PLUGIN_MANIFEST_FQN) ?: return

            val id = annotation.stringArg("id")
            val rawName = annotation.stringArg("name")
            val version = annotation.stringArg("version")
            val minAppVersion = annotation.stringArg("minAppVersion")
            val maxAppVersion = annotation.stringArg("maxAppVersion").ifBlank { null }
            val description = annotation.stringArg("description")
            val icon = annotation.stringArg("icon").ifBlank { descriptorIcon.orEmpty() }.ifBlank { null }
            val license = annotation.stringArg("license")
            val entryClass = pluginClass.kotlinFqName.asString()
            val displayName = if (rawName.isBlank() || rawName == "<auto>") id else rawName

            val authorCall = annotation.getValueArgument(Name.identifier("author")) as? IrConstructorCall
            val authorName = authorCall?.stringArg("name").orEmpty()
            val hasAuthor = authorName.isNotBlank()

            val linksCall = annotation.getValueArgument(Name.identifier("links")) as? IrConstructorCall
            val linksSource = linksCall?.stringArg("source").orEmpty().ifBlank { null }
            val linksIssues = linksCall?.stringArg("issues").orEmpty().ifBlank { null }
            val linksWebsite = linksCall?.stringArg("website").orEmpty().ifBlank { null }
            val linksDonate = linksCall?.stringArg("donate").orEmpty().ifBlank { null }
            val hasLinks = listOfNotNull(linksSource, linksIssues, linksWebsite, linksDonate).isNotEmpty()

            val permissions = (annotation.getValueArgument(Name.identifier("permissions")) as? IrVararg)
                ?.elements
                ?.mapNotNull { (it as? IrConst)?.value as? String }
                ?: emptyList()

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
                    arguments[8] = if (hasAuthor) {
                        buildPluginAuthorCall(pluginContext, authorName, authorCall)
                    } else null
                    arguments[9] = license.asIrString()
                    arguments[10] = if (hasLinks) {
                        buildPluginLinksCall(pluginContext, linksSource, linksIssues, linksWebsite, linksDonate)
                    } else null
                    arguments[11] = buildStringListLiteral(pluginContext, permissions)
                }
            }

            descriptorProperty.backingField?.initializer = null
            val getter = descriptorProperty.getter ?: return
            getter.body = pluginContext.irFactory.createExpressionBody(constructorCall)

            descriptorOutputDir?.let { dir ->
                writeDescriptorJson(
                    dir, id, displayName, version, minAppVersion, maxAppVersion, entryClass,
                    description, icon,
                    author = if (hasAuthor) authorName to authorCall else null,
                    license,
                    links = if (hasLinks) listOf(linksSource, linksIssues, linksWebsite, linksDonate) else null,
                    permissions
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
            outputDir: String, id: String, name: String, version: String,
            minAppVersion: String, maxAppVersion: String?, entryClass: String,
            description: String, icon: String?,
            author: Pair<String, IrConstructorCall?>?, license: String,
            links: List<String?>?, permissions: List<String>
        ) {
            val dir = File(outputDir).apply { mkdirs() }
            fun esc(s: String) = s.replace("\"", "\\\"")

            val json = buildString {
                appendLine("{")
                appendLine("  \"id\": \"${esc(id)}\",")
                appendLine("  \"name\": \"${esc(name)}\",")
                appendLine("  \"version\": \"${esc(version)}\",")
                appendLine("  \"minAppVersion\": \"${esc(minAppVersion)}\",")
                if (maxAppVersion != null) appendLine("  \"maxAppVersion\": \"${esc(maxAppVersion)}\",")
                appendLine("  \"entryClass\": \"${esc(entryClass)}\",")
                appendLine("  \"description\": \"${esc(description)}\",")
                if (icon != null) appendLine("  \"icon\": \"${esc(icon)}\",")
                author?.let { (authorName, call) ->
                    appendLine("  \"author\": {")
                    appendLine(
                        "    \"name\": \"${esc(authorName)}\"" + listOfNotNull(
                            call?.stringArg("email")?.ifBlank { null }?.let { "\"email\": \"${esc(it)}\"" },
                            call?.stringArg("url")?.ifBlank { null }?.let { "\"url\": \"${esc(it)}\"" },
                            call?.stringArg("github")?.ifBlank { null }?.let { "\"github\": \"${esc(it)}\"" }
                        ).joinToString("") { ",\n    $it" })
                    appendLine("\n  },")
                }
                appendLine("  \"license\": \"${esc(license)}\",")
                links?.let { (source, issues, website, donate) ->
                    val fields = listOfNotNull(
                        source?.let { "\"source\": \"${esc(it)}\"" },
                        issues?.let { "\"issues\": \"${esc(it)}\"" },
                        website?.let { "\"website\": \"${esc(it)}\"" },
                        donate?.let { "\"donate\": \"${esc(it)}\"" }
                    )
                    appendLine("  \"links\": { ${fields.joinToString(", ")} },")
                }
                appendLine("  \"permissions\": [${permissions.joinToString(", ") { "\"${esc(it)}\"" }}]")
                appendLine("}")
            }
            File(dir, "plugin.json").writeText(json)
            messageCollector.report(
                CompilerMessageSeverity.INFO,
                "Klyx compiler plugin: wrote generated plugin.json for '$id'"
            )
        }

        private fun buildPluginAuthorCall(
            pluginContext: IrPluginContext,
            name: String,
            authorCall: IrConstructorCall?
        ): IrConstructorCall {
            val authorClass = finder.findClass(KlyxPluginIds.PLUGIN_AUTHOR_CLASS_ID)
                ?: error("PluginAuthor not found on classpath")

            val ctor = authorClass.constructors.first()
            return IrConstructorCallImpl.fromSymbolOwner(authorClass.defaultType, ctor).apply {
                context(pluginContext) {
                    arguments[0] = name.asIrString()
                    arguments[1] = authorCall?.stringArgOrNull("email")?.asIrString()
                    arguments[2] = authorCall?.stringArgOrNull("url")?.asIrString()
                    arguments[3] = authorCall?.stringArgOrNull("github")?.asIrString()
                }
            }
        }

        private fun buildPluginLinksCall(
            pluginContext: IrPluginContext,
            source: String?, issues: String?, website: String?, donate: String?
        ): IrConstructorCall {
            val linksClass = finder.findClass(KlyxPluginIds.PLUGIN_LINKS_CLASS_ID)
                ?: error("PluginLinks not found on classpath")
            val ctor = linksClass.constructors.first()

            return IrConstructorCallImpl.fromSymbolOwner(linksClass.defaultType, ctor).apply {
                context(pluginContext) {
                    arguments[0] = source?.asIrString()
                    arguments[1] = issues?.asIrString()
                    arguments[2] = website?.asIrString()
                    arguments[3] = donate?.asIrString()
                }
            }
        }

        private fun buildStringListLiteral(pluginContext: IrPluginContext, values: List<String>): IrExpression {
            val finder = pluginContext.finderForBuiltins()
            if (values.isEmpty()) {
                val emptyListFn = finder.findFunctions(
                    CallableId(FqName("kotlin.collections"), Name.identifier("emptyList"))
                ).first()
                return IrCallImpl.fromSymbolOwner(-1, -1, emptyListFn).apply {
                    typeArguments[0] = pluginContext.irBuiltIns.stringType
                }
            }

            val listOfFn = finder.findFunctions(
                CallableId(FqName("kotlin.collections"), Name.identifier("listOf"))
            ).first { it.owner.parameters.firstOrNull()?.isVararg == true }

            return IrCallImpl.fromSymbolOwner(-1, -1, listOfFn).apply {
                typeArguments[0] = pluginContext.irBuiltIns.stringType
                arguments[0] = IrVarargImpl(
                    -1, -1,
                    type = pluginContext.irBuiltIns.arrayClass.typeWith(pluginContext.irBuiltIns.stringType),
                    varargElementType = pluginContext.irBuiltIns.stringType,
                    elements = values.map { context(pluginContext) { it.asIrString() } }
                )
            }
        }
    }
}
