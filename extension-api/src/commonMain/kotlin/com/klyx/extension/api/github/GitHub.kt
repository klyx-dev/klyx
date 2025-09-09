package com.klyx.extension.api.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val client = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

suspend fun latestGithubRelease(
    repoNameWithOwner: String,
    requireAssets: Boolean = false,
    preRelease: Boolean = false
): GithubRelease {
    val url = "https://api.github.com/repos/$repoNameWithOwner/releases"
    val releases: List<GithubRelease> = client.get(url) {
        headers {
            append(HttpHeaders.UserAgent, "ktor-client")
        }
    }.body()

    val release = releases.firstOrNull {
        (!requireAssets || it.assets.isNotEmpty()) && it.preRelease == preRelease
    } ?: error("No matching release found")

    return release.copy(
        assets = release.assets.map { asset ->
            asset.copy(
                digest = asset.digest?.removePrefix("sha256:")
            )
        }
    )
}

suspend fun getReleaseByTagName(
    repoNameWithOwner: String,
    tag: String
): GithubRelease {
    val url = "https://api.github.com/repos/$repoNameWithOwner/releases/tags/$tag"
    return client.get(url) {
        headers {
            append(HttpHeaders.UserAgent, "ktor-client")
        }
    }.body()
}

fun buildAssetUrl(repoNameWithOwner: String, tag: String, kind: AssetKind): String {
    val encodedTag = tag.encodeURLPathPart()
    return "https://github.com/$repoNameWithOwner/archive/refs/tags/$encodedTag.${kind.ext}"
}
