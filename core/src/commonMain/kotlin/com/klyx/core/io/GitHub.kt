package com.klyx.core.io

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class GithubRelease(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("prerelease")
    val preRelease: Boolean,
    val assets: List<GithubReleaseAsset>,
    @SerialName("tarball_url")
    val tarballUrl: String,
    @SerialName("zipball_url")
    val zipballUrl: String
)

@Serializable
data class GithubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val digest: String? = null
)


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
