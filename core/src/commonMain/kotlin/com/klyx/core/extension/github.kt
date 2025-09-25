package com.klyx.core.extension

import com.akuleshov7.ktoml.Toml
import com.klyx.core.Environment
import com.klyx.core.fetchBody
import com.klyx.core.fetchText
import com.klyx.core.file.KxFile
import com.klyx.core.logging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.ExperimentalTime

private const val BASE_RAW_URL = "https://raw.githubusercontent.com/klyx-dev/extensions/main"
const val EXTENSIONS_INDEX_URL = "$BASE_RAW_URL/extensions.toml"

private const val BASE_GITHUB_API_EXTENSIONS_URL = "https://api.github.com/repos/klyx-dev/extensions/contents"

private val logger = logger("GithubApi")

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
    return if (parts.size >= 2) {
        parts[0] to parts[1]
    } else error("Invalid repository URL format: $url")
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
            logger.error { "Failed to process extension $name: ${e.message}" }
        }
    }
}.flowOn(Dispatchers.IO)

private suspend fun fetchSingleExtension(name: String, entry: ExtensionEntry): ExtensionInfo {
    val submoduleMetaUrl = "$BASE_GITHUB_API_EXTENSIONS_URL/${entry.submodule}"
    val submoduleInfoJson = try {
        fetchText(submoduleMetaUrl)
    } catch (e: Exception) {
        logger.error { "Failed to fetch submodule metadata for '$name' from $submoduleMetaUrl: ${e.message}" }
        throw ExtensionFetchException("Failed to fetch submodule metadata for '$name'. URL: $submoduleMetaUrl", e)
    }

    val submoduleInfo = Json.parseToJsonElement(submoduleInfoJson).jsonObject

    val submoduleHtmlUrl = submoduleInfo["html_url"]?.jsonPrimitive?.contentOrNull
        ?: throw ExtensionFetchException("Missing 'html_url' in submodule metadata for $name from $submoduleMetaUrl")
    
    val submoduleSha = submoduleInfo["sha"]?.jsonPrimitive?.contentOrNull
        ?: throw ExtensionFetchException("Missing 'sha' (commit hash) in submodule metadata for $name from $submoduleMetaUrl")

    val (owner, repo) = try {
        parseRepoInfo(submoduleHtmlUrl)
    } catch (e: Exception) {
        logger.error { "Failed to parse owner/repo from submodule html_url '$submoduleHtmlUrl' for extension $name: ${e.message}"}
        throw ExtensionFetchException("Could not parse repository owner and name from submodule HTML URL: $submoduleHtmlUrl", e)
    }

    val tomlFileUrl = "https://raw.githubusercontent.com/$owner/$repo/$submoduleSha/extension.toml"

    try {
        val tomlContentRaw = fetchText(tomlFileUrl)
        return Toml.decodeFromString(tomlContentRaw)
    } catch (e: Exception) {
        logger.error { "Failed to fetch or parse extension.toml for '$name' from $tomlFileUrl: ${e.message}" }
        throw ExtensionFetchException("Failed to fetch or parse extension.toml for '$name'. URL: $tomlFileUrl", e)
    }
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

@OptIn(ExperimentalTime::class)
suspend fun fetchLastUpdated(repo: String): LocalDateTime? {
    val (username, reponame) = parseRepoInfo(repo)
    val jsonText = fetchText("https://api.github.com/repos/$username/$reponame")
    val json = Json.parseToJsonElement(jsonText).jsonObject
    val pushedAt = json["pushed_at"]?.jsonPrimitive?.content ?: return null
    return kotlin.time.Instant.parse(pushedAt).toLocalDateTime(TimeZone.currentSystemDefault())
}

internal expect suspend fun ByteArray.extractRepoZip(targetDir: KxFile)
