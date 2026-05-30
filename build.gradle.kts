// Top-level build file where you can add configuration options common to all sub-projects/modules.
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

tasks.register("generateGrammarFiles") {
    description = "Generate tree-sitter grammar files for project"

    dependsOn(subprojects.flatMap { project ->
        project.tasks.matching { it.name == "generateGrammarFiles" }
    })
}

subprojects {
    tasks.matching { it.name == "generateGrammarFiles" }.configureEach {
        doLast {
            val baseDir = (property("generatedSrc") as DirectoryProperty).get().asFile
            val targetDir = baseDir.resolve("androidMain/kotlin")

            if (!targetDir.exists()) {
                println("Directory not found in module [${project.name}], skipping actual keyword removal.")
                return@doLast
            }

            val foldersToDelete = listOf("commonMain", "jvmMain", "nativeMain", "nativeInterop")
            foldersToDelete.forEach { folderName ->
                val dir = baseDir.resolve(folderName)
                if (dir.exists()) {
                    dir.deleteRecursively()
                    println("Deleted unnecessary KMP folder: $folderName")
                }
            }

            targetDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val originalContent = file.readText()
                    var cleanedContent = originalContent.replace(Regex("""\bactual\s+"""), "")
                    cleanedContent = cleanedContent.replace(Regex("""\bexpect\s+"""), "")

                    if (originalContent != cleanedContent) {
                        file.writeText(cleanedContent)
                        println("Cleaned KMP keywords in [${project.name}] -> ${file.name}")
                    }
                }

            val grammarName = project.name

            val scanner = projectDir.resolve("tree-sitter-$grammarName/src/scanner.c")

            if (scanner.exists()) {
                val cmake = baseDir.resolveSibling("CMakeLists.txt")
                val original = cmake.readText()

                val parserLine = "../../tree-sitter-$grammarName/src/parser.c)"
                val replacement =
                    """
                ../../tree-sitter-$grammarName/src/parser.c
                ../../tree-sitter-$grammarName/src/scanner.c)
                """.trimIndent()

                val patched = original.replace(parserLine, replacement)

                if (patched != original) {
                    cmake.writeText(patched)
                }
            }
        }
    }
}
