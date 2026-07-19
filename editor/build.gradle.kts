import java.util.Locale

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotest)
}

android {
    namespace = "com.klyx.editor"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xreturn-value-checker=check",
        )
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.sora.editor)
    implementation(libs.ktreesitter)

    rootProject.project("languages").subprojects.forEach {
        implementation(project(":languages:${it.name}"))
    }

    testImplementation(libs.junit)
    testImplementation(libs.bundles.kotest.unit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.bundles.kotest.android)
}

abstract class GenerateTreeSitterTask : DefaultTask() {

    @get:Input
    abstract val languageModules: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val baseOutputDir = outputDir.get().asFile

        val editorFolder = baseOutputDir.resolve("com/klyx/editor")
        editorFolder.mkdirs()

        val outputFileTreeSitter = File(editorFolder, "TreeSitter.kt")
        val outputFileRegistry = File(editorFolder, "TSLanguageRegistry.kt")

        val modules = languageModules.get()

        val aliasMap = mapOf(
            "jsx" to "javascript"
        )

        val extMap = mapOf(
            "c" to listOf("c", "h"),
            "cpp" to listOf("cpp", "cc", "cxx", "hpp", "hh", "hxx"),
            "javascript" to listOf("js"),
            "jsx" to listOf("jsx"),
            "typescript" to listOf("ts"),
            "tsx" to listOf("tsx"),
            "html" to listOf("html", "htm"),
            "python" to listOf("py"),
            "rust" to listOf("rs"),
            "kotlin" to listOf("kt", "kts"),
        )

        // Accumulators for TreeSitter.kt
        val tsImports = StringBuilder()
        val tsFunctions = StringBuilder()
        val tsWhenBranches = StringBuilder()

        // Accumulators for TSLanguageRegistry.kt
        val registryImports = StringBuilder()
        val supplierBranches = StringBuilder()

        for (lang in modules) {
            val capName = lang.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val className = "TreeSitter$capName"
            val packageName = "com.klyx.languages.$lang"

            tsImports.appendLine("import $packageName.$className")
            tsFunctions.appendLine("    fun ${lang}() = createEditorLanguage(\"$lang\", $className.language())")

            registryImports.appendLine("import $packageName.$className")
            supplierBranches.appendLine("        \"$lang\" to { $className.language() },")

            aliasMap.forEach { (aliasName, baseModule) ->
                if (baseModule == lang) {
                    tsFunctions.appendLine("    fun ${aliasName}() = createEditorLanguage(\"$aliasName\", $className.language())")
                    supplierBranches.appendLine("        \"$aliasName\" to { $className.language() },")
                }
            }
        }

        val allActiveTargets = modules.toMutableList()
        aliasMap.forEach { (aliasName, baseModule) ->
            if (modules.contains(baseModule)) {
                allActiveTargets.add(aliasName)
            }
        }

        for (target in allActiveTargets) {
            val extensions = extMap[target] ?: listOf(target)
            val extListStr = extensions.joinToString(", ") { "\"$it\"" }
            tsWhenBranches.appendLine("            $extListStr -> ${target}()")
        }

        outputFileTreeSitter.writeText(
            """
            |package com.klyx.editor
            |
            |import android.content.Context
            |import com.klyx.editor.treesitter.createEditorLanguage
            |import com.klyx.editor.treesitter.DynamicLanguageProvider
            |import com.klyx.editor.treesitter.EditorLanguage
            |import com.klyx.editor.treesitter.LanguageQueries
            |import io.github.rosemoe.sora.lang.Language
            |import io.github.rosemoe.sora.lang.EmptyLanguage
            |import java.util.concurrent.ConcurrentHashMap
            |
            |$tsImports
            |/** AUTO-GENERATED CLASS: Do not edit manually! */
            |class TreeSitter(private val context: Context) : AutoCloseable {
            |    val languageProvider = DynamicLanguageProvider(TSLanguageRegistry(context))
            |    private val dynamicExtensions = ConcurrentHashMap<String, String>()
            |    private val dynamicFileNames = ConcurrentHashMap<String, String>()
            |    private val dynamicLanguages = ConcurrentHashMap<String, EditorLanguage>()
            |    
            |$tsFunctions
            |    private fun createEditorLanguage(name: String, language: Any): Language =
            |        createEditorLanguage(context, name, language, languageProvider)
            |
            |    fun getLanguageForExtension(extension: String): Language {
            |        val ext = extension.lowercase()
            |        val builtIn = when(ext) {
            |$tsWhenBranches            else -> null
            |        }
            |        if (builtIn != null) return builtIn
            |        val dynamicName = dynamicExtensions[ext]
            |            ?: return dynamicFileNames[ext]?.let { dynamicLanguages[it] } ?: EmptyLanguage()
            |        return dynamicLanguages[dynamicName] ?: EmptyLanguage()
            |    }
            |
            |    fun getLanguageForFileName(fileName: String): Language {
            |        val name = fileName.lowercase()
            |        return dynamicFileNames[name]?.let { langName ->
            |            dynamicLanguages[langName]
            |        } ?: getLanguageForExtension(name.substringAfterLast('.', ""))
            |    }
            |
            |    fun registerDynamicLanguage(
            |        name: String,
            |        extensions: List<String>,
            |        fileNames: List<String>,
            |        editorLanguage: EditorLanguage,
            |        queries: LanguageQueries,
            |    ) {
            |        val normalized = name.lowercase()
            |        dynamicLanguages[normalized] = editorLanguage
            |        languageProvider.registerLanguage(normalized, editorLanguage.tsLanguage, queries)
            |        extensions.forEach { ext -> dynamicExtensions[ext.lowercase()] = normalized }
            |        fileNames.forEach { fn -> dynamicFileNames[fn.lowercase()] = normalized }
            |    }
            |
            |    fun unregisterDynamicLanguage(name: String) {
            |        val normalized = name.lowercase()
            |        dynamicLanguages.remove(normalized)
            |        languageProvider.unregisterLanguage(normalized)
            |        dynamicExtensions.values.removeAll { it == normalized }
            |        dynamicFileNames.values.removeAll { it == normalized }
            |    }
            |
            |    override fun close() {
            |        dynamicLanguages.values.forEach { it.destroy() }
            |        dynamicLanguages.clear()
            |        languageProvider.clear()
            |    }
            |}
            """.trimMargin()
        )

        outputFileRegistry.writeText(
            """
            |package com.klyx.editor
            |
            |import android.content.Context
            |import com.klyx.editor.treesitter.LanguageProvider
            |import com.klyx.editor.treesitter.LanguageQueries
            |import com.klyx.editor.treesitter.closeSafely
            |import io.github.treesitter.ktreesitter.Language
            |import java.util.concurrent.ConcurrentHashMap
            |
            |$registryImports
            |/** AUTO-GENERATED CLASS: Do not edit manually! */
            |class TSLanguageRegistry(private val context: Context) : LanguageProvider {
            |
            |    private val languageCache = ConcurrentHashMap<String, Language>()
            |    private val queriesCache = ConcurrentHashMap<String, LanguageQueries>()
            |
            |    private val languageSuppliers = mapOf(
            |$supplierBranches
            |    )
            |
            |    override fun getLanguage(languageName: String): Language? {
            |        val normalizedName = languageName.lowercase()
            |        return languageCache.getOrPut(normalizedName) {
            |            val nativePointer = languageSuppliers[normalizedName]?.invoke() ?: return null
            |            Language(nativePointer)
            |        }
            |    }
            |
            |    override fun getQueries(languageName: String): LanguageQueries? {
            |        val normalizedName = languageName.lowercase()
            |        val lang = getLanguage(normalizedName) ?: return null
            |
            |        return queriesCache.getOrPut(normalizedName) {
            |            LanguageQueries(context, lang, normalizedName)
            |        }
            |    }
            |
            |    fun clear() {
            |        queriesCache.values.forEach { it.closeSafely() }
            |        queriesCache.clear()
            |        languageCache.clear()
            |    }
            |}
            """.trimMargin()
        )
    }
}

val generateTreeSitterRegistry = tasks.register<GenerateTreeSitterTask>("generateTreeSitterRegistry") {
    group = "build setup"
    description = "Generates the TreeSitter class dynamically based on installed submodules"

    val activeModules = provider {
        rootProject.project("languages").subprojects.map { it.name.removePrefix("tree-sitter-") }
    }

    languageModules.set(activeModules)
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateTreeSitterRegistry,
            GenerateTreeSitterTask::outputDir
        )
    }
}
