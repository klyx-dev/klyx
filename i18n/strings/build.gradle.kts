import com.google.devtools.ksp.gradle.KspAATask
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Locale

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}

val generateI18nStrings = tasks.register<GenerateI18nStringsTask>("generateI18nStrings") {
    group = "i18n"
    description = "Generates the Strings interface and per-language objects from translations/*.json"
    translationFiles.from(fileTree(layout.projectDirectory.dir("translations")))
    outputDir.set(layout.buildDirectory.dir("generated/sources/i18n"))
}

android {
    namespace = "com.klyx.i18n.strings"

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

    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateI18nStrings,
            GenerateI18nStringsTask::outputDir
        )
    }
}

ksp {
    arg("klyx.i18n.generateStringsProperty", "true")
}

// The KSP task does not consume AGP variant generated source directories on its
// own, so the generated Translations.kt must be added to its source roots explicitly.
tasks.withType<KspAATask>().configureEach {
    kspConfig.sourceRoots.from(generateI18nStrings.map { it.outputDir })
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidx.compose)

    implementation(projects.i18n.runtime)
    ksp(projects.i18n.processor)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.kotest.unit)
}

abstract class GenerateI18nStringsTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val translationFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val jsonFiles = translationFiles.files.filter { it.isFile && it.extension == "json" }

        val schemaFile = jsonFiles.firstOrNull { it.name == SCHEMA_FILE }
            ?: throw GradleException("i18n: missing $SCHEMA_FILE in the translations directory")

        @Suppress("UNCHECKED_CAST")
        val schema = JsonSlurper().parse(schemaFile) as Map<String, Any>
        val defaultTag = schema[DEFAULT_LANGUAGE_TAG_KEY] as? String
            ?: throw GradleException("i18n: \"$DEFAULT_LANGUAGE_TAG_KEY\" is required in $SCHEMA_FILE")
        @Suppress("UNCHECKED_CAST")
        val params = schema[PARAMS_KEY] as? Map<String, Map<String, String>> ?: emptyMap()

        val languages = jsonFiles
            .filterNot { it.name.startsWith("_") }
            .associate { it.nameWithoutExtension to readStrings(it) }

        val defaultStrings = languages[defaultTag]
            ?: throw GradleException("i18n: default language file \"$defaultTag.json\" not found")

        params.keys.filterNot { it in defaultStrings }.forEach { key ->
            throw GradleException("i18n: $SCHEMA_FILE declares params for \"$key\" which does not exist in $defaultTag.json")
        }

        defaultStrings.forEach { (key, value) ->
            val declared = params[key]?.keys.orEmpty()
            val used = placeholdersIn(value).map { it.first }.toSet()
            val unused = declared - used
            if (unused.isNotEmpty()) {
                throw GradleException("i18n: $SCHEMA_FILE declares unused param(s) $unused for \"$key\"")
            }
        }

        languages.forEach { (tag, strings) ->
            if (tag == defaultTag) return@forEach
            strings.forEach { (key, value) ->
                if (key !in defaultStrings) {
                    throw GradleException("i18n: $tag.json defines \"$key\" which does not exist in $defaultTag.json")
                }
                val expected = placeholdersIn(defaultStrings.getValue(key)).map { it.first }.toSet()
                val actual = placeholdersIn(value).map { it.first }.toSet()
                val missing = expected - actual
                if (missing.isNotEmpty()) {
                    logger.warn("i18n: $tag.json \"$key\" is missing placeholder(s) $missing — the value will render as \"null\"")
                }
            }
        }

        val output = buildString {
            appendLine("// Generated from translations/*.json by the generateI18nStrings task. DO NOT EDIT.")
            appendLine("package com.klyx.i18n.strings")
            appendLine()
            appendLine("import com.klyx.i18n.I18nStrings")
            appendLine()
            appendLine("// Untranslated strings fall back to the default language ($defaultTag).")
            appendLine("interface Strings {")
            defaultStrings.keys.forEach { key ->
                appendLine("    ${interfaceProperty(key, params[key])}")
            }
            appendLine("}")
            appendLine()

            languages.forEach { (tag, strings) ->
                val annotation = if (tag == defaultTag) {
                    "@I18nStrings(languageTag = \"$tag\", default = true)"
                } else {
                    "@I18nStrings(languageTag = \"$tag\")"
                }
                appendLine(annotation)
                appendLine("object ${classNameFor(tag)} : Strings {")
                strings.forEach { (key, value) ->
                    appendLine("    ${objectProperty(key, value, params[key].orEmpty(), tag)}")
                }
                appendLine("}")
                appendLine()
            }
        }

        outputDir.get().asFile
            .resolve("com/klyx/i18n/strings")
            .apply { mkdirs() }
            .resolve("Translations.kt")
            .writeText(output)
    }

    private fun readStrings(file: File): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        val raw = JsonSlurper().parse(file) as Map<String, Any>
        return raw.entries.associate { (key, value) ->
            if (!KEY_REGEX.matches(key)) {
                throw GradleException("i18n: ${file.name} has invalid key \"$key\" (expected a lowerCamelCase identifier)")
            }
            val text = value as? String
                ?: throw GradleException("i18n: ${file.name} \"$key\" must be a string")
            if (text.isBlank()) {
                throw GradleException("i18n: ${file.name} \"$key\" must not be empty")
            }
            key to text
        }
    }

    private fun interfaceProperty(key: String, paramTypes: Map<String, String>?): String =
        "val $key: ${propertyType(paramTypes)} get() = EnStrings.$key"

    private fun propertyType(paramTypes: Map<String, String>?): String =
        if (paramTypes.isNullOrEmpty()) {
            "String"
        } else {
            "(${paramTypes.entries.joinToString(", ") { "${it.key}: ${it.value}" }}) -> String"
        }

    private fun objectProperty(key: String, value: String, paramTypes: Map<String, String>, tag: String): String {
        val template = toKotlinTemplate(key, value, paramTypes, tag)
        return if (paramTypes.isEmpty()) {
            "override val $key = $template"
        } else {
            val lambdaParams = paramTypes.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            "override val $key = { $lambdaParams -> $template }"
        }
    }

    private fun toKotlinTemplate(key: String, value: String, paramTypes: Map<String, String>, tag: String): String {
        val template = StringBuilder("\"")
        var index = 0

        PLACEHOLDER_REGEX.findAll(value).forEach { match ->
            template.append(escapeLiteral(value.substring(index, match.range.first)))
            index = match.range.last + 1

            val name = match.groupValues[1]
            val fallback = match.groupValues[2]
            val type = paramTypes[name]
                ?: throw GradleException("i18n: $tag.json \"$key\" uses placeholder {$name} but it is not declared in $SCHEMA_FILE")
            if (fallback.isNotEmpty() && type != NULLABLE_STRING) {
                throw GradleException("i18n: $tag.json \"$key\" uses a fallback for {$name} which is not a nullable parameter")
            }
            val expression = if (fallback.isNotEmpty()) {
                "$name ?: \"${escapeLiteral(fallback)}\""
            } else {
                name
            }
            template.append("\${").append(expression).append("}")
        }
        template.append(escapeLiteral(value.substring(index)))
        template.append("\"")
        return template.toString()
    }

    private fun placeholdersIn(value: String): List<Pair<String, String>> =
        PLACEHOLDER_REGEX.findAll(value).map { it.groupValues[1] to it.groupValues[2] }.toList()

    private fun escapeLiteral(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun classNameFor(tag: String): String =
        tag.split("-", "_").joinToString("") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.uppercase(Locale.ROOT) else it.toString() }
        }.filter { it.isLetterOrDigit() } + "Strings"

    private companion object {
        val KEY_REGEX = Regex("[a-z][A-Za-z0-9]*")
        val PLACEHOLDER_REGEX = Regex("\\{([a-zA-Z][a-zA-Z0-9_]*)(?::([^{}]*))?\\}")

        const val SCHEMA_FILE = "_schema.json"
        const val DEFAULT_LANGUAGE_TAG_KEY = "defaultLanguageTag"
        const val PARAMS_KEY = "params"
        const val NULLABLE_STRING = "String?"
    }
}
