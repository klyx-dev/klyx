package com.klyx.wasm.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.Modifier

class HostProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    companion object {
        private const val SPACE = " "
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val modules = resolver.getSymbolsWithAnnotation("com.klyx.wasm.annotations.HostModule")
            .filterIsInstance<KSClassDeclaration>()
        if (!modules.iterator().hasNext()) return emptyList()

        for (module in modules) {
            val wasmModuleName = module.annotations
                .first { it.shortName.asString() == "HostModule" }
                .arguments.first().value as String

            val pkg = module.packageName.asString()
            val functions = module.getAllFunctions()
                .onEach { it.checkVisibility() }
                .filter { fn ->
                    fn.annotations.any { annotation ->
                        annotation.shortName.asString() == "HostFunction"
                    }
                }

            generateHostModule(resolver, module, pkg, wasmModuleName.toKotlinLiteral(), functions)
        }

        return emptyList()
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth", "UnsafeCallOnNullableType")
    private fun generateHostModule(
        resolver: Resolver,
        module: KSClassDeclaration,
        packageName: String,
        wasmModuleName: String,
        functions: Sequence<KSFunctionDeclaration>
    ) {
        val moduleName = module.simpleName.asString()
        val lowerName = moduleName.lowercase()
        val isClass = module.classKind == ClassKind.CLASS
        val name = if (isClass) lowerName else moduleName

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, module.containingFile!!),
            packageName = packageName,
            fileName = "${moduleName}Module"
        )

        file.bufferedWriter().use { writer ->
            writer.appendLine("// DO NOT EDIT")
            writer.appendLine("// This file is generated automatically")
            writer.appendLine()
            writer.appendLine("@file:OptIn(ExperimentalWasmApi::class)")
            writer.appendLine()
            writer.appendLine("package $packageName")
            writer.appendLine()

            val imports = mutableSetOf<String>()

            imports += "com.klyx.wasm.ExperimentalWasmApi"
            imports += "com.klyx.wasm.HostModule"
            imports += "com.klyx.wasm.HostModuleScope"
            imports += "com.klyx.wasm.signature"

            for (fn in functions) {
                if (fn.parameters.any {
                        it.type.resolve()
                            .declaration
                            .qualifiedName
                            ?.asString() == "com.klyx.wasm.type.WasmString"
                    }
                ) {
                    imports += "com.klyx.wasm.type.WasmString"
                }

                if (fn.parameters.any {
                        it.type.resolve()
                            .declaration
                            .qualifiedName
                            ?.asString()
                            ?.startsWith("com.klyx.wasm.type.") == true
                    }
                ) {
                    imports += "com.klyx.wasm.type.toWasm"
                }

                if (fn.returnType?.resolve()?.declaration?.qualifiedName?.asString() == "kotlin.String") {
                    imports += "com.klyx.wasm.internal.toUtf8ByteArray"
                }

                if (fn.returnType != null) {
                    imports += "com.klyx.wasm.toWasmValue"
                }
            }

            writer.appendLine(imports.sorted().joinToString("\n") { "import $it" })
            writer.appendLine()

            val primaryConstructor = module.primaryConstructor
            val hasRequiredArgs = primaryConstructor
                ?.parameters
                ?.any { !it.hasDefault && it.name != null } == true

            val moduleDeclLine = if (hasRequiredArgs) {
                "class ${moduleName}Module(private val $lowerName: $moduleName) : HostModule {"
            } else {
                "object ${moduleName}Module : HostModule {"
            }

            writer.appendLine(moduleDeclLine)
            writer.appendLine("    override val name = \"$wasmModuleName\"")

            if (!hasRequiredArgs && isClass) {
                writer.appendLine("    private val $lowerName = $moduleName()")
            }

            writer.appendLine()
            writer.appendLine("    override fun HostModuleScope.functions() {")

            for ((idx, fn) in functions.withIndex()) {
                val fnName = fn.simpleName.asString()
                val hostFuncAnnotation = fn.annotations.first {
                    it.annotationType.resolve()
                        .declaration
                        .qualifiedName
                        ?.asString() == "com.klyx.wasm.annotations.HostFunction"
                }

                val nameArg = hostFuncAnnotation.arguments
                    .firstOrNull { it.name?.asString() == "name" }
                    ?.value as? String

                val caseArgName = hostFuncAnnotation.arguments
                    .firstOrNull { it.name?.asString() == "case" }
                    ?.value
                    ?.toString()
                    ?.substringAfterLast(".")

                val exportName = when {
                    !nameArg.isNullOrEmpty() -> nameArg
                    else -> when (caseArgName) {
                        "KebabCase" -> fnName.kebabcase()
                        "SnakeCase" -> fnName.snakecase()
                        "ScreamingSnakeCase" -> fnName.snakecase().uppercase()
                        "CamelCase" -> fnName.camelcase()
                        "PascalCase" -> fnName.pascalcase()
                        else -> fnName.kebabcase()
                    }
                }

                val hasExtensionReceiver = fn.extensionReceiver?.resolve() != null

                val paramTypes = fn.parameters
                    .map { it.type.resolve().toWasmType() }
                    .flatten()

                val wasmParams = paramTypes.let { types ->
                    if (types.isNotEmpty()) {
                        "listOf(${types.joinToString()})"
                    } else "nothing"
                }

                val returns = fn.returnType?.resolve().toWasmType()
                val wasmReturns = returns.let { types ->
                    if (types.isNotEmpty()) {
                        "listOf(${types.joinToString()})"
                    } else "nothing"
                }

                val hasReturnType = returns.isNotEmpty()

                val fnCall = generateCallLine(resolver, hasExtensionReceiver, fn, name)
                val returnVarName = "value"

                writer.appendLine(SPACE.repeat(8) + "function(")
                writer.appendLine(SPACE.repeat(12) + "name = \"$exportName\",")
                writer.appendLine(SPACE.repeat(12) + "signature { $wasmParams returns $wasmReturns }")
                writer.appendLine(SPACE.repeat(8) + ") {")
                writer.appendLine(SPACE.repeat(12) + "with(memory) {")

                if (hasExtensionReceiver) {
                    writer.appendLine(SPACE.repeat(16) + "with($name) {")
                }

                val extraSpace = if (hasExtensionReceiver) SPACE.repeat(4) else ""

                writer.appendLine(
                    extraSpace + SPACE.repeat(16) + if (hasReturnType) "val $returnVarName = $fnCall" else fnCall
                )

                writer.appendLine(
                    extraSpace + SPACE.repeat(16) + if (hasReturnType) {
                        generateReturnValue(
                            resolver = resolver,
                            fn = fn,
                            returnVarName = returnVarName
                        )
                    } else {
                        "emptyList()"
                    }
                )

                if (hasExtensionReceiver) {
                    writer.appendLine(SPACE.repeat(16) + "}")
                }

                writer.appendLine(SPACE.repeat(12) + "}")
                writer.appendLine(SPACE.repeat(8) + "}")

                if (idx != functions.count() - 1) writer.appendLine()
            }

            writer.appendLine(SPACE.repeat(4) + "}")
            writer.appendLine("}")
        }
    }

    private fun KSType?.toWasmType(): List<String> {
        val wasmTypes = mutableListOf<String?>()
        val type = this?.unwrapTypeAlias()

        if (type != null) {
            val name = type.declaration
                .qualifiedName
                ?.asString()

            wasmTypes += when (name?.removePrefix("com.klyx.wasm.type.")) {
                "WasmUByte", "WasmByte", "WasmUShort", "WasmShort", "WasmUInt", "WasmInt" -> "i32"
                "WasmLong", "WasmULong" -> "i64"
                "WasmFloat" -> "f32"
                "WasmDouble" -> "f64"
                else -> null
            }

            if (name == "com.klyx.wasm.type.WasmString" || name == "kotlin.String") {
                wasmTypes += listOf("i32", "i32")
            }

            wasmTypes += when (name?.removePrefix("kotlin.")) {
                "UByte", "Byte", "UShort", "Short", "UInt", "Int", "Boolean" -> "i32"
                "Long", "ULong" -> "i64"
                "Float" -> "f32"
                "Double" -> "f64"
                else -> null
            }
        }

        return wasmTypes.filterNotNull()
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    private fun generateCallLine(
        resolver: Resolver,
        hasExtensionReceiver: Boolean,
        fn: KSFunctionDeclaration,
        moduleVar: String
    ): String {
        val wasmValueType = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("com.klyx.wasm.type.WasmType")
        )?.asStarProjectedType()

        val argsList = mutableListOf<String>()

        fn.parameters.forEach { param ->
            if (param.name != null) {
                val type = param.type.resolve().unwrapTypeAlias()
                val qualifiedName = type.declaration.qualifiedName?.asString()

                val argExpr = when {
                    qualifiedName == "com.klyx.wasm.WasmMemory" -> "memory"
                    qualifiedName == "com.klyx.wasm.WasmInstance" -> "instance"
                    qualifiedName == "com.klyx.wasm.FunctionScope" -> "this@function"

                    wasmValueType != null && wasmValueType.isAssignableFrom(type) -> {
                        if (qualifiedName == "com.klyx.wasm.type.WasmString") {
                            "WasmString(takeInt(), takeInt())"
                        } else "takeInt().toWasm()"
                    }

                    else -> when (qualifiedName?.removePrefix("kotlin.")) {
                        "UByte" -> "takeByte().toUByte()"
                        "Byte" -> "takeByte()"
                        "UShort" -> "takeShort().toUShort()"
                        "Short" -> "takeShort()"
                        "UInt" -> "takeUInt()"
                        "Int" -> "takeInt()"
                        "Long" -> "takeLong()"
                        "ULong" -> "takeULong()"
                        "Float" -> "takeFloat()"
                        "Double" -> "takeDouble()"
                        "Boolean" -> "takeBoolean()"
                        "String" -> "readUtf8String(takeInt(), takeInt())"
                        else -> "take()"
                    }
                }

                argsList += argExpr
            }
        }

        return "${if (!hasExtensionReceiver) "$moduleVar." else ""}${fn.simpleName.asString()}(${argsList.joinToString()})"
    }

    private fun generateReturnValue(
        resolver: Resolver,
        fn: KSFunctionDeclaration,
        returnVarName: String = "value"
    ): String {
        val wasmValueType = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("com.klyx.wasm.type.WasmType")
        )?.asStarProjectedType()

        var value: String
        val type = fn.returnType?.resolve()?.unwrapTypeAlias() ?: return "emptyListOf()"

        if (wasmValueType != null && wasmValueType.isAssignableFrom(type)) {
            if (type.declaration.qualifiedName?.asString() == "com.klyx.wasm.type.WasmString") {
                return "listOf($returnVarName.pointer.toWasmValue(), $returnVarName.length.toWasmValue())"
            }
            value = "$returnVarName.value.toWasmValue()"
            return "listOf($value)"
        }

        val qualifiedName = type.declaration.qualifiedName?.asString()

        if (qualifiedName == "kotlin.String") {
            return """
                val (ptr, len) = memory.allocateAndWrite($returnVarName.toUtf8ByteArray())
                ${SPACE.repeat(16)}listOf(ptr.toWasmValue(), len.toWasmValue())
            """.trimIndent()
        }

        value = when (qualifiedName?.removePrefix("kotlin.")) {
            "UByte", "Byte", "UShort", "Short", "UInt", "Int", "Long", "ULong" -> "$returnVarName.toWasmValue()"
            else -> returnVarName
        }

        return "listOf($value)"
    }

    fun KSType.unwrapTypeAlias(): KSType {
        var type = this
        while (type.declaration is KSTypeAlias) {
            val alias = type.declaration as KSTypeAlias
            type = alias.type.resolve()
        }
        return type
    }

    private fun KSFunctionDeclaration.checkVisibility() {
        if (hasHostFunctionAnnotation()) {
            val isNotAccessible = modifiers.contains(Modifier.PRIVATE)

            if (isNotAccessible) {
                logger.error("@HostFunction can only be applied to public members", this)
            }
        }
    }

    private fun KSFunctionDeclaration.hasHostFunctionAnnotation(): Boolean {
        val qualifiedName = "com.klyx.wasm.annotations.HostFunction"
        return annotations.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
        }
    }

    private fun String.kebabcase(): String =
        trim()
            .replace(Regex("([a-z])([A-Z])"), "$1-$2")
            .replace(Regex("[\\s_]+"), "-")
            .lowercase()

    private fun String.snakecase(): String =
        trim()
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[\\s-]+"), "_")
            .lowercase()

    private fun String.camelcase(): String {
        val parts = this.kebabcase().split('-', '_')
        return parts.first() + parts.drop(1)
            .joinToString("") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
    }

    private fun String.pascalcase(): String {
        val parts = this.kebabcase().split('-', '_')
        return parts.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun String.toKotlinLiteral(): String {
        return this.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
    }
}
