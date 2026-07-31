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

        data class TargetInfo(val className: String, val packageName: String, val extensions: List<String>)

        val targetInfos = mutableMapOf<String, TargetInfo>()

        for (lang in modules) {
            val capName = lang.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val className = "TreeSitter$capName"
            val packageName = "com.klyx.languages.$lang"
            val extensions = extMap[lang] ?: listOf(lang)

            targetInfos[lang] = TargetInfo(className, packageName, extensions)

            aliasMap.forEach { (aliasName, baseModule) ->
                if (baseModule == lang) {
                    val aliasExts = extMap[aliasName] ?: listOf(aliasName)
                    targetInfos[aliasName] = TargetInfo(className, packageName, aliasExts)
                }
            }
        }

        // Accumulators for TreeSitter.kt
        val tsFunctions = StringBuilder()

        // Accumulators for TSLanguageRegistry.kt
        val registryImports = StringBuilder()
        val supplierBranches = StringBuilder()
        val extBranches = StringBuilder()

        val addedImports = mutableSetOf<String>()

        for ((targetName, info) in targetInfos) {
            val (className, packageName, extensions) = info
            val importLine = "import $packageName.$className"

            if (addedImports.add(importLine)) {
                registryImports.appendLine(importLine)
            }
            supplierBranches.appendLine("        \"$targetName\" to { $className.language() },")
            extBranches.appendLine("        \"$targetName\" to listOf(${extensions.joinToString(", ") { "\"$it\"" }}),")

            val primaryExt = extensions.first()
            tsFunctions.appendLine("    fun ${targetName}(): Language = getLanguageForExtension(\"$primaryExt\")")
        }

        outputFileTreeSitter.writeText(
            """
            |package com.klyx.editor
            |
            |import android.content.Context
            |import com.klyx.editor.treesitter.DynamicLanguageProvider
            |import com.klyx.editor.treesitter.EditorLanguage
            |import com.klyx.editor.treesitter.LanguageEntry
            |import com.klyx.editor.treesitter.LanguagePriority
            |import com.klyx.editor.treesitter.LanguageQueries
            |import com.klyx.editor.treesitter.QuerySources
            |import com.klyx.editor.treesitter.editorTheme
            |import io.github.rosemoe.sora.lang.Language
            |import io.github.rosemoe.sora.lang.EmptyLanguage
            |import java.util.concurrent.ConcurrentHashMap
            |
            |/** AUTO-GENERATED CLASS: Do not edit manually! */
            |class TreeSitter(private val context: Context) : AutoCloseable {
            |    val languageProvider = DynamicLanguageProvider()
            |    private val builtInRegistry = TSLanguageRegistry(context, languageProvider)
            |
            |$tsFunctions
            |    fun getLanguageForExtension(extension: String): Language {
            |        val ext = extension.lowercase()
            |        val entry = languageProvider.getEntryForExtension(ext) ?: return EmptyLanguage()
            |        return buildEditorLanguage(entry)
            |    }
            |
            |    fun getLanguageForFileName(fileName: String): Language {
            |        val name = fileName.lowercase()
            |        val entry = languageProvider.getEntryForFileName(name)
            |        if (entry != null) {
            |            return buildEditorLanguage(entry)
            |        }
            |        return getLanguageForExtension(name.substringAfterLast('.', ""))
            |    }
            |
            |    private fun buildEditorLanguage(entry: LanguageEntry): EditorLanguage {
            |        val queries = LanguageQueries.fromSource(
            |            language = entry.language,
            |            languageName = entry.name,
            |            highlights = entry.querySources.highlights,
            |            indents = entry.querySources.indents,
            |            folds = entry.querySources.folds,
            |            locals = entry.querySources.locals,
            |            injections = entry.querySources.injections,
            |            tags = entry.querySources.tags,
            |        )
            |        val editorLang = EditorLanguage(
            |            tsLanguage = entry.language,
            |            queries = { queries },
            |            languageProvider = languageProvider,
            |            themeDescription = { editorTheme() }
            |        )
            |        if (entry.themeOverrides.isNotEmpty()) {
            |            editorLang.applyThemeOverrides(entry.themeOverrides)
            |        }
            |        return editorLang
            |    }
            |
            |    fun registerDynamicLanguage(
            |        name: String,
            |        extensions: List<String>,
            |        fileNames: List<String>,
            |        language: io.github.treesitter.ktreesitter.Language,
            |        querySources: QuerySources,
            |        themeOverrides: Map<String, Long> = emptyMap(),
            |    ): Boolean {
            |        return languageProvider.register(
            |            name = name,
            |            language = language,
            |            querySources = querySources,
            |            extensions = extensions,
            |            fileNames = fileNames,
            |            priority = LanguagePriority.PLUGIN,
            |            themeOverrides = themeOverrides,
            |        )
            |    }
            |
            |    fun unregisterDynamicLanguage(name: String) {
            |        languageProvider.unregister(name)
            |    }
            |
            |    override fun close() {
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
            |import com.klyx.editor.treesitter.DynamicLanguageProvider
            |import com.klyx.editor.treesitter.LanguagePriority
            |import com.klyx.editor.treesitter.LanguageQueries
            |import com.klyx.editor.treesitter.QuerySources
            |import io.github.treesitter.ktreesitter.Language
            |
            |$registryImports
            |/** AUTO-GENERATED CLASS: Do not edit manually! */
            |class TSLanguageRegistry(context: Context, provider: DynamicLanguageProvider) {
            |
            |    init {
            |        val suppliers = mapOf<String, () -> Any>(
            |$supplierBranches
            |        )
            |
            |        val extMap = mapOf<String, List<String>>(
            |$extBranches
            |        )
            |
            |        for ((name, supplier) in suppliers) {
            |            val tsLanguage = Language(supplier())
            |            val querySources = LanguageQueries.loadQuerySources(context, name)
            |            val extensions = extMap[name] ?: listOf(name)
            |            provider.register(
            |                name = name,
            |                language = tsLanguage,
            |                querySources = querySources,
            |                extensions = extensions,
            |                fileNames = emptyList(),
            |                priority = LanguagePriority.BUILT_IN,
            |            )
            |        }
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
