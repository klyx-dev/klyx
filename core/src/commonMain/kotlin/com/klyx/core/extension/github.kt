package com.klyx.core.extension

import com.akuleshov7.ktoml.Toml
import com.klyx.core.Environment
import com.klyx.core.fetchBody
import com.klyx.core.fetchText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.codec.binary.Base64
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File

const val BASE_RAW_URL = "https://raw.githubusercontent.com/klyx-dev/extensions/main"
const val EXTENSIONS_INDEX_URL = "$BASE_RAW_URL/extensions.toml"

const val BASE_GITHUB_API_EXTENSIONS_URL = "https://api.github.com/repos/klyx-dev/extensions/contents"

fun parseRepoInfo(url: String): Pair<String, String> {
    val cleanUrl = url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("git@")
        .removePrefix("github.com:")
        .removePrefix("github.com/")
        .removeSuffix(".git")
        .removeSuffix("/")

    val parts = cleanUrl.split("/")
    return if (parts.size == 2) {
        parts[0] to parts[1]
    } else error("Invalid")
}

suspend fun downloadRepoZip(repo: String, branch: String = "main"): ByteArray {
    val url = "https://github.com/$repo/archive/refs/heads/$branch.zip"
    return fetchBody(url)
}

suspend fun fetchExtensionEntries(): ExtensionsIndex {
    val raw = fetchText(EXTENSIONS_INDEX_URL)
    return Toml.decodeFromString(raw)
}

suspend fun fetchExtensions(): Result<List<ExtensionToml>> = withContext(Dispatchers.IO) {
    val extensions = mutableListOf<ExtensionToml>()
    val entries = fetchExtensionEntries()

    for ((name, entry) in entries) {
        val submoduleInfo = Json.parseToJsonElement(
            fetchText("$BASE_GITHUB_API_EXTENSIONS_URL/${entry.submodule}")
        ).jsonObject

        submoduleInfo["git_url"]?.jsonPrimitive?.contentOrNull?.let { gitUrl ->
            val repoInfo = Json.parseToJsonElement(fetchText(gitUrl)).jsonObject
            val tomlUrl = repoInfo["tree"]?.jsonArray?.find {
                it.jsonObject["path"]?.jsonPrimitive?.contentOrNull == "extension.toml"
            }?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: return@withContext Result.failure(
                ExtensionFetchException("Failed to fetch extension.toml URL for $name")
            )

            val tomlContent = Json.parseToJsonElement(
                fetchText(tomlUrl)
            ).jsonObject["content"]?.jsonPrimitive?.contentOrNull ?: return@withContext Result.failure(
                ExtensionFetchException("Failed to fetch extension.toml content for $name")
            )

            extensions.add(Toml.decodeFromString(Base64.decodeBase64(tomlContent).decodeToString()))
        } ?: return@withContext Result.failure(ExtensionFetchException("Failed to fetch extension Git URL for $name"))
    }

    Result.success(extensions)
}

suspend fun installExtension(toml: ExtensionToml): Result<File> = withContext(Dispatchers.IO) {
    if (toml.repository.isBlank()) {
        return@withContext Result.failure(ExtensionInstallException("Extension repository is blank"))
    }

    val (username, reponame) = parseRepoInfo(toml.repository)
    val zip = downloadRepoZip(repo = "$username/$reponame")
    val internalDir = File(Environment.ExtensionsDir, toml.id)

    zip.extractRepoZip(internalDir)
    Result.success(internalDir)
}

private suspend fun ByteArray.extractRepoZip(targetDir: File): Boolean = withContext(Dispatchers.IO) {
    ZipArchiveInputStream(inputStream()).use { input ->
        var entry = input.nextEntry

        while (entry != null) {
            val name = entry.name.substringAfter("/")

            if (name.isEmpty()) {
                entry = input.nextEntry
                continue
            }

            val outputFile = File(targetDir, name)

            if (entry.isDirectory) {
                outputFile.mkdirs()
            } else {
                outputFile.parentFile?.mkdirs()
                outputFile.outputStream().use(input::copyTo)
            }

            entry = input.nextEntry
        }
    }

    true
}
