package com.klyx.build

import java.nio.file.Path
import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun generateBuildInfo(
    projectRoot: String,
    @Output outputDir: Path
) {
    fun git(vararg args: String) = runCatching {
        val process = ProcessBuilder("git", "-C", projectRoot, *args)
            .redirectErrorStream(true)
            .start()

        val result = process.inputStream.bufferedReader().use { it.readText().trim() }

        if (process.waitFor() == 0) result else null
    }.getOrNull()?.takeIf { it.isNotBlank() }

    fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    val commit = git("rev-parse", "HEAD")
        ?: System.getenv("GITHUB_SHA")
        ?: System.getenv("CI_COMMIT_SHA")
        ?: "unknown"

    val branch = git("branch", "--show-current")
        ?: System.getenv("GITHUB_REF_NAME")
        ?: System.getenv("CI_COMMIT_REF_NAME")
        ?: "unknown"

    val androidModule = Path(projectRoot) / "android-app/module.yaml"
    val yaml = Yaml(SafeConstructor(LoaderOptions()))
    val root = androidModule.bufferedReader().use {
        yaml.load<Any?>(it)
    } as? Map<*, *> ?: error("Invalid module.yaml")

    val settings = root["settings"] as? Map<*, *> ?: error("Missing settings in ${androidModule.fileName}")
    val android = settings["android"] as? Map<*, *> ?: error("Missing settings.android")

    val versionName = android["versionName"] as? String
        ?: error("Missing or invalid settings.android.versionName")

    val versionCode = (android["versionCode"] as? Number)?.toInt()

    val buildNumber = versionCode?.toString() ?: System.getenv("BUILD_NUMBER") ?: "local"

    val source = """
        package com.klyx.build
        
        object BuildInfo {
            const val COMMIT = "${escape(commit)}"
            const val BRANCH = "${escape(branch)}"
            const val BUILD_NUMBER = "${escape(buildNumber)}"
            const val VERSION_NAME = "${escape(versionName)}"
        }
    """.trimIndent()

    outputDir.createDirectories()

    val outputFile = outputDir.resolve("BuildInfo.kt")

    if (!outputFile.exists() || outputFile.readText() != source) {
        outputFile.writeText(source)
    }
}
