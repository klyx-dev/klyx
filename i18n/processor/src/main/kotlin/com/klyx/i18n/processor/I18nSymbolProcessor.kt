package com.klyx.i18n.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.validate

internal class I18nSymbolProcessor(
    private val configs: Configs,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val declarations = mutableListOf<KSAnnotated>()
    private val processedDeclarations = mutableSetOf<String>()

    private val visitor = I18nVisitor(declarations)
    private var hasGeneratedCode: Boolean = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        declarations.clear()
        val i18nSymbols = resolver.getSymbolsWithAnnotation(ANNOTATION_PACKAGE)
            .filter { (it is KSPropertyDeclaration || it is KSClassDeclaration) && it.validate() }
            .toList()

        i18nSymbols.forEach { it.accept(visitor, Unit) }

        val newDeclarations = declarations.filterIsInstance<KSDeclaration>().filterNot { dec ->
            val key = "${dec.qualifiedName?.asString()}#${dec.containingFile?.fileName}"
            processedDeclarations.contains(key)
        }

        newDeclarations.forEach { dec ->
            val key = "${dec.qualifiedName?.asString()}#${dec.containingFile?.fileName}"
            processedDeclarations.add(key)
        }

        if (validate(newDeclarations).not()) return emptyList()

        val fileName = "${configs.moduleName.toUpperCamelCase()}Strings"

        val stringsName = "${configs.moduleName.toLowerCamelCase()}Strings"

        val visibility = if (configs.internalVisibility) "internal" else "public"

        val stringsProperty = if (configs.generateStringsProperty) {
            """
            |$visibility val strings: $fileName
            |    @Composable
            |    get() = Local$fileName.current
            """.trimMargin()
        } else {
            ""
        }

        val defaultLanguageTag = newDeclarations
            .firstNotNullOfOrNull { it.annotations.getDefaultLanguageTag() }
            ?.let { "\"$it\"" }
            ?: "Locale.current.toLanguageTag()"

        val defaultStrings = newDeclarations
            .first { it.annotations.getValue<Boolean>(ANNOTATION_PARAM_DEFAULT) == true }

        val packagesOutput = newDeclarations
            .mapNotNull { it.qualifiedName?.asString() }
            .plus(defaultStrings.getClassQualifiedName())
            .joinToString(separator = "\n") { packageName -> "import $packageName" }

        val stringsClassOutput = defaultStrings.getClassSimpleName()

        val defaultStringsOutput = defaultStrings.simpleName.getShortName()

        val translationMappingOutput = newDeclarations
            .map {
                it.annotations.getValue<String>(ANNOTATION_PARAM_LANGUAGE_TAG)!! to it.simpleName.getShortName()
            }.joinToString(",\n") { (languageTag, property) ->
                "$INDENTATION\"$languageTag\" to $property"
            }

        codeGenerator.createNewFile(
            dependencies = Dependencies(
                aggregating = true,
                sources = newDeclarations.map { it.containingFile!! }.toTypedArray()
            ),
            packageName = configs.packageName,
            fileName = fileName
        ).use { stream ->
            stream.write(
                """
                |package ${configs.packageName}
                |
                |import androidx.compose.runtime.Composable
                |import androidx.compose.runtime.ProvidableCompositionLocal
                |import androidx.compose.runtime.staticCompositionLocalOf
                |import androidx.compose.ui.text.intl.Locale
                |import com.klyx.i18n.I18n
                |import com.klyx.i18n.LanguageTag
                |import com.klyx.i18n.rememberStrings
                |import com.klyx.i18n.ProvideStrings
                |$packagesOutput
                |
                |$visibility val $stringsName: Map<LanguageTag, $stringsClassOutput> = mapOf(
                |$translationMappingOutput
                |)
                |
                |$visibility val Local$fileName: ProvidableCompositionLocal<$stringsClassOutput> = 
                |    staticCompositionLocalOf { $defaultStringsOutput }
                |
                |$stringsProperty
                |
                |@Composable
                |$visibility fun remember$fileName(
                |    defaultLanguageTag: LanguageTag = $defaultLanguageTag,
                |    currentLanguageTag: LanguageTag = Locale.current.toLanguageTag(),
                |): I18n<$stringsClassOutput> =
                |    rememberStrings($stringsName, defaultLanguageTag, currentLanguageTag)
                |
                |@Composable
                |$visibility fun Provide$fileName(
                |    i18n: I18n<$stringsClassOutput> = remember$fileName(),
                |    content: @Composable () -> Unit
                |) {
                |    ProvideStrings(i18n, Local$fileName, content)
                |}
                |
                |$visibility fun getLocale$fileName(locale: Locale = Locale.current): $stringsClassOutput {
                |    return $stringsName[locale.toLanguageTag()] ?: $defaultStringsOutput
                |}                
                """.trimMargin().toByteArray()
            )
        }

        hasGeneratedCode = true

        return emptyList()
    }

    private fun validate(properties: List<KSDeclaration>): Boolean {
        val defaultCount = properties
            .count { it.annotations.getValue<Boolean>(ANNOTATION_PARAM_DEFAULT) == true }

        val differentTypeCount = properties
            .groupBy { it.getClassQualifiedName() }
            .count()

        return when {
            properties.isEmpty() -> {
                // No new declarations to process in this round
                false
            }

            hasGeneratedCode && properties.all { dec ->
                val key = "${dec.qualifiedName?.asString()}#${dec.containingFile?.fileName}"
                processedDeclarations.contains(key)
            } -> {
                // KSP 2.0 Incremental Compilation Optimization
                // Skip processing when all declarations have been handled in previous rounds.
                // This prevents redundant code generation while maintaining correctness.
                false
            }

            defaultCount == 0 -> {
                logger.warn("No @I18nStrings(default = true) found")
                false
            }

            defaultCount > 1 -> {
                logger.exception(IllegalArgumentException("More than one @I18nStrings(default = true) found"))
                false
            }

            differentTypeCount != 1 -> {
                logger.exception(IllegalArgumentException("All @I18nStrings must have the same type"))
                false
            }

            else -> true
        }
    }


    private fun KSDeclaration.getClassSimpleName(): String? = when (this) {
        is KSPropertyDeclaration -> getter?.returnType?.resolve()?.declaration?.simpleName?.asString()
        is KSClassDeclaration -> superTypes
            .map { it.resolve().declaration }
            .firstOrNull { it.simpleName.asString() != "Any" }
            ?.simpleName?.asString()
        else -> null
    }

    private fun KSDeclaration.getClassQualifiedName(): String? = when (this) {
        is KSPropertyDeclaration -> getter?.returnType?.resolve()?.declaration?.qualifiedName?.asString()
        is KSClassDeclaration -> superTypes
            .map { it.resolve().declaration }
            .firstOrNull { it.simpleName.asString() != "Any" }
            ?.qualifiedName?.asString()
        else -> null
    }

    private fun Sequence<KSAnnotation>.getDefaultLanguageTag(): String? =
        firstOrNull {
            withName(ANNOTATION_NAME)
                ?.arguments
                ?.withName(ANNOTATION_PARAM_DEFAULT)
                ?.value == true
        }?.arguments
            ?.withName(ANNOTATION_PARAM_LANGUAGE_TAG)
            ?.value as? String

    private inline fun <reified T> Sequence<KSAnnotation>.getValue(argumentName: String): T? =
        withName(ANNOTATION_NAME)
            ?.arguments
            ?.withName(argumentName)
            ?.value as? T

    private fun Sequence<KSAnnotation>.withName(name: String): KSAnnotation? =
        firstOrNull { it.shortName.getShortName() == name }

    private fun List<KSValueArgument>.withName(name: String): KSValueArgument? =
        firstOrNull { it.name?.getShortName() == name }

    private companion object {
        val INDENTATION = " ".repeat(4)

        const val ANNOTATION_NAME = "I18nStrings"
        const val ANNOTATION_PACKAGE = "com.klyx.i18n.$ANNOTATION_NAME"
        const val ANNOTATION_PARAM_LANGUAGE_TAG = "languageTag"
        const val ANNOTATION_PARAM_DEFAULT = "default"
    }
}
