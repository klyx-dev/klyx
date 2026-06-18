import com.android.build.api.dsl.LibraryExtension
import io.github.treesitter.ktreesitter.plugin.GrammarExtension
import io.github.treesitter.ktreesitter.plugin.GrammarFilesTask
import java.util.Locale

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotest) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktreesitter) apply false
}

tasks.register("prepareTreeSitter") {
    group = "build setup"
    description = "Generate all tree-sitter grammar files"
    dependsOn(
        subprojects
            .filter { it.path.startsWith(":languages:tree-sitter-") }
            .map { it.tasks.named("generateGrammarFiles") }
    )
}

subprojects {
    if (path.startsWith(":languages:tree-sitter-")) {
        pluginManager.apply(rootProject.libs.plugins.ktreesitter.get().pluginId)
        pluginManager.apply(rootProject.libs.plugins.android.library.get().pluginId)

        val langName = project.name.removePrefix("tree-sitter-")
        val capitalizedName = langName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        val versionStr = projectDir.resolve("Makefile").let { makefile ->
            if (makefile.exists()) {
                makefile.useLines { lines ->
                    lines.firstOrNull { it.startsWith("VERSION := ") }?.removePrefix("VERSION := ") ?: "0.0.1"
                }
            } else "0.0.1"
        }

        extensions.configure<GrammarExtension> {
            baseDir = projectDir
            grammarName = langName
            className = "TreeSitter$capitalizedName"
            packageName = "com.klyx.languages.$langName"
        }

        val generateTask = tasks.named<GrammarFilesTask>("generateGrammarFiles")

        generateTask.configure {
            doLast {
                val genSrcDir = generatedSrc.get().asFile

                val targetDir = genSrcDir.resolve("androidMain/kotlin")
                if (targetDir.exists()) {
                    listOf("commonMain", "jvmMain", "nativeMain", "nativeInterop").forEach { folderName ->
                        val dir = genSrcDir.resolve(folderName)
                        if (dir.exists()) {
                            dir.deleteRecursively()
                            println("[${project.name}] Deleted unnecessary KMP folder: $folderName")
                        }
                    }

                    targetDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                        val originalContent = file.readText()
                        var cleanedContent = originalContent.replace(Regex("""\bactual\s+"""), "")
                        cleanedContent = cleanedContent.replace(Regex("""\bexpect\s+"""), "")

                        if (originalContent != cleanedContent) {
                            file.writeText(cleanedContent)
                            println("[${project.name}] Cleaned KMP keywords in -> ${file.name}")
                        }
                    }
                } else {
                    println("[${project.name}] Kotlin target directory not found, skipping KMP cleanup.")
                }

                val bindingFile = genSrcDir.resolve("jni/binding.c")
                val cmakeFile = cmakeListsFile.get().asFile

                if (bindingFile.exists()) {
                    var content = bindingFile.readText()
                    val includeTarget = "#include <tree-sitter-${langName}.h>"

                    val hasTreeSitterSubdir =
                        projectDir.resolve("bindings/c/tree_sitter/tree-sitter-${langName}.h").exists() ||
                                projectDir.resolve("src/tree_sitter/tree-sitter-${langName}.h").exists() ||
                                projectDir.parentFile.resolve("bindings/c/tree_sitter/tree-sitter-${langName}.h")
                                    .exists()

                    val hasAnyHeader = hasTreeSitterSubdir ||
                            projectDir.resolve("bindings/c/tree-sitter-${langName}.h").exists() ||
                            projectDir.resolve("src/tree-sitter-${langName}.h").exists()

                    if (hasTreeSitterSubdir && content.contains(includeTarget)) {
                        content = content.replace(includeTarget, "#include <tree_sitter/tree-sitter-${langName}.h>")
                    } else if (!hasAnyHeader && content.contains(includeTarget)) {
                        // grammar doesn't provide a header! remove the include and declare it manually.
                        val externDecl = """
                        #ifdef __cplusplus
                        extern "C" {
                        #endif
                        void *tree_sitter_$langName();
                        #ifdef __cplusplus
                        }
                        #endif
                        """.trimIndent()
                        content = content.replace(includeTarget, externDecl)
                    }
                    bindingFile.writeText(content)
                }

                if (cmakeFile.exists()) {
                    var cmakeContent = cmakeFile.readText()

                    // some languages use scanner.c, some use scanner.cc
                    val scannerC = projectDir.resolve("src/scanner.c")
                    val scannerCc = projectDir.resolve("src/scanner.cc")
                    if (scannerC.exists() && !cmakeContent.contains("src/scanner.c")) {
                        cmakeContent = cmakeContent.replace(
                            "src/parser.c)",
                            $$"src/parser.c\n        ${CMAKE_CURRENT_SOURCE_DIR}/../../src/scanner.c)"
                        )
                    } else if (scannerCc.exists() && !cmakeContent.contains("src/scanner.cc")) {
                        cmakeContent = cmakeContent.replace(
                            "src/parser.c)",
                            $$"src/parser.c\n        ${CMAKE_CURRENT_SOURCE_DIR}/../../src/scanner.cc)"
                        )
                    }

                    val includePaths = mutableListOf($$"${CMAKE_CURRENT_SOURCE_DIR}/../../src")
                    if (projectDir.parentFile.resolve("bindings/c").exists()) {
                        includePaths.add($$"${CMAKE_CURRENT_SOURCE_DIR}/../../../bindings/c")
                    }

                    // ensure src/ is in the include directories so <tree_sitter/parser.h> resolves
                    val includeDirString =
                        $$"target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../../src)"
                    if (!cmakeContent.contains(includeDirString)) {
                        cmakeContent += "\n\n$includeDirString"
                    }

                    cmakeFile.writeText(cmakeContent)
                }
            }
        }

        configure<LibraryExtension> {
            namespace = "com.klyx.languages.$langName"
            ndkVersion = property("ndk.version") as String

            @Suppress("UnstableApiUsage")
            compileSdk {
                version = release(37)
            }

            defaultConfig {
                minSdk = 28
                resValue("string", "version", versionStr)

                ndk {
                    abiFilters += listOf("arm64-v8a", "x86_64")
                }
            }

            externalNativeBuild {
                cmake {
                    path = generateTask.get().cmakeListsFile.get().asFile
                    buildStagingDirectory = file(".cmake")
                    version = property("cmake.version") as String
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }

            buildFeatures {
                resValues = true
            }

            sourceSets {
                getByName("main") {
                    val genSrcDir = generateTask.get().generatedSrc.get().asFile
                    kotlin.directories += genSrcDir.resolve("androidMain/kotlin").absolutePath
                }
            }
        }
    }
}
