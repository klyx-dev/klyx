package com.klyx.core.extension

import com.akuleshov7.ktoml.Toml
import com.klyx.core.Environment
import com.klyx.core.decodeBase64
import com.klyx.core.fetchBody
import com.klyx.core.fetchText
import com.klyx.core.file.KxFile
import com.klyx.core.logging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val BASE_RAW_URL = "https://raw.githubusercontent.com/klyx-dev/extensions/main"
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

fun fetchExtensionsFlow() = flow {
    val extensionsIndex = fetchExtensionEntries()

    for ((name, entry) in extensionsIndex) {
        try {
            val extension = fetchSingleExtension(name, entry)
            emit(extension)
        } catch (e: Exception) {
            logger().error { "Failed to fetch extension $name: ${e.message}" }
        }
    }
}.flowOn(Dispatchers.IO)

private suspend fun fetchSingleExtension(name: String, entry: ExtensionEntry): ExtensionInfo {
    val submoduleInfo = Json.parseToJsonElement(
        fetchText("$BASE_GITHUB_API_EXTENSIONS_URL/${entry.submodule}")
    ).jsonObject

    val gitUrl = submoduleInfo["git_url"]?.jsonPrimitive?.contentOrNull
        ?: throw ExtensionFetchException("Failed to fetch extension Git URL for $name")

    val repoInfo = Json.parseToJsonElement(fetchText(gitUrl)).jsonObject

    val tomlUrl = repoInfo["tree"]?.jsonArray?.find {
        it.jsonObject["path"]?.jsonPrimitive?.contentOrNull == "extension.toml"
    }?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        ?: throw ExtensionFetchException("Failed to fetch extension.toml URL for $name")

    val tomlContent = Json.parseToJsonElement(fetchText(tomlUrl)).jsonObject["content"]
        ?.jsonPrimitive?.contentOrNull
        ?: throw ExtensionFetchException("Failed to fetch extension.toml content for $name")

    return Toml.decodeFromString(decodeBase64(tomlContent).decodeToString())
}

suspend fun installExtension(toml: ExtensionInfo): Result<KxFile> = withContext(Dispatchers.IO) {
    if (toml.repository.isBlank()) {
        return@withContext Result.failure(ExtensionInstallException("Extension repository is blank"))
    }

    val (username, reponame) = parseRepoInfo(toml.repository)

    val zip = try {
        downloadRepoZip(repo = "$username/$reponame")
    } catch (e: Exception) {
        return@withContext Result.failure(e)
    }
    val internalDir = KxFile("${Environment.ExtensionsDir}/${toml.id}")

    zip.extractRepoZip(internalDir)
    Result.success(internalDir)
}

suspend fun fetchLastUpdated(repo: String): LocalDateTime? {
    val (username, reponame) = parseRepoInfo(repo)
    val jsonText = fetchText("https://api.github.com/repos/$username/$reponame")
    val json = Json.parseToJsonElement(jsonText).jsonObject
    val pushedAt = json["pushed_at"]?.jsonPrimitive?.content ?: return null
    return Instant.parse(pushedAt).toLocalDateTime(TimeZone.currentSystemDefault())
}

internal expect suspend fun ByteArray.extractRepoZip(targetDir: KxFile)
