package com.klyx.build

import java.nio.file.Path
import java.util.Properties
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedReader
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
@TaskAction
fun generateSources(
    @Input propertiesFile: Path,
    @Output generatedSourceDir: Path
) {
    // clean the old state if any is present from the previous invocation
    generatedSourceDir.deleteRecursively()

    val outputFile = generatedSourceDir / "properties.kt"

    if (!propertiesFile.isRegularFile()) {
        error("The file $propertiesFile does not exist")
    }
    println("Generating sources")

    val properties = propertiesFile.bufferedReader().use { reader ->
        Properties().apply { load(reader) }
    }.toMap()

    // need to ensure the output directory structure exists:
    // the Kotlin Toolchain doesn't pre-create it for us
    outputFile.createParentDirectories()

    val code = buildString {
        appendLine("package com.klyx.build.generated")
        appendLine("public object Config {")
        for ((key, value) in properties) {
            val v = if (value is String) "\"$value\"" else value
            appendLine("    const val `$key`: ${value::class.simpleName} = $v")
        }
        appendLine("}")
    }

    outputFile.writeText(code)
}
