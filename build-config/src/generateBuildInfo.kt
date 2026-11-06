package com.klyx.build

import java.nio.file.Path
import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun generateBuildInfo(
    projectRoot: String,
    version: String?,
    @Output outputDir: Path
) {
    fun git(vararg args: String) = runCatching {
        val process = ProcessBuilder("git", "-C", projectRoot, *args)
            .redirectErrorStream(true)
            .start()

        val result = process.inputStream.bufferedReader().use { it.readText().trim() }

        if (process.waitFor() == 0) result else null
    }.getOrNull()?.takeIf { it.isNotBlank() }

    val commit = git("rev-parse", "HEAD")
        ?: System.getenv("GITHUB_SHA")
        ?: System.getenv("CI_COMMIT_SHA")
        ?: "unknown"

    val branch = git("branch", "--show-current")
        ?: System.getenv("GITHUB_REF_NAME")
        ?: System.getenv("CI_COMMIT_REF_NAME")
        ?: "unknown"

    val buildNumber = version ?: System.getenv("BUILD_NUMBER") ?: "local"

    val source = """
        package com.klyx.build
        
        object BuildInfo {
            const val COMMIT = "$commit"
            const val BRANCH = "$branch"
            const val BUILD_NUMBER = "$buildNumber"
        }
    """.trimIndent()

    outputDir.createDirectories()

    val outputFile = outputDir.resolve("BuildInfo.kt")

    if (!outputFile.exists() || outputFile.readText() != source) {
        outputFile.writeText(source)
    }
}
