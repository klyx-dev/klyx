package com.klyx.core.extension

import com.klyx.core.fetchText
import com.klyx.core.file.KxFile
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalTime::class)
suspend fun fetchLastUpdated(repo: String): LocalDateTime? {
    val (username, reponame) = parseRepoInfo(repo)
    val jsonText = fetchText("https://api.github.com/repos/$username/$reponame")
    val json = Json.parseToJsonElement(jsonText).jsonObject
    val pushedAt = json["pushed_at"]?.jsonPrimitive?.content ?: return null
    return kotlin.time.Instant.parse(pushedAt).toLocalDateTime(TimeZone.currentSystemDefault())
}

expect suspend fun ByteArray.extractRepoZip(targetDir: KxFile)
