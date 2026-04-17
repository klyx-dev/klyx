@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.ui.page.extension

import com.klyx.core.KlyxBuildConfig
import com.klyx.core.fetchBody
import com.klyx.core.fetchText
import com.klyx.core.httpClient
import com.klyx.core.io.fs
import com.klyx.core.logging.log
import com.klyx.extension.nodegraph.ExtensionMetadata
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.io.encoding.Base64

private const val STORE_INDEX_URL = "https://klyx-dev.github.io/extensions/index.json"
private const val WORKER_PUBLISH_URL = "https://publish-extension.klyx.workers.dev"

@Serializable
data class StoreRegistry(
    val storeVersion: Int,
    val lastUpdated: String,
    val extensions: List<StoreExtension>
)

@Serializable
data class StoreExtension(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val downloadUrl: String,
    val downloadCount: Int = 0
)

@Serializable
data class PublishPayload(
    val metadata: String,
    val fileBase64: String
)

class ExtensionStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    suspend fun fetchDownloadCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            json.decodeFromString(fetchText("$WORKER_PUBLISH_URL/downloads"))
        } catch (e: Exception) {
            log.error { "Failed to fetch download counts: ${e.message}" }
            emptyMap()
        }
    }

    suspend fun incrementDownloadCount(extensionId: String) = withContext(Dispatchers.IO) {
        try {
            httpClient.post("$WORKER_PUBLISH_URL/download?id=$extensionId")
        } catch (e: Exception) {
            log.error { "Failed to increment download count for $extensionId: ${e.message}" }
        }
    }

    suspend fun fetchStoreIndex(): List<StoreExtension> = withContext(Dispatchers.IO) {
        try {
            val str = fetchText(STORE_INDEX_URL)
            json.decodeFromString<StoreRegistry>(str).extensions
        } catch (e: Exception) {
            log.error { "Failed to fetch store index: ${e.message}" }
            emptyList()
        }
    }

    suspend fun downloadExtension(ext: StoreExtension, destDir: Path): Path? = withContext(Dispatchers.IO) {
        try {
            if (!fs.exists(destDir)) fs.createDirectories(destDir)
            val targetFile = Path(destDir, "${ext.id}.kxext")
            val bytes = fetchBody(ext.downloadUrl)
            fs.sink(targetFile).buffered().use { it.write(bytes) }

            launch { incrementDownloadCount(ext.id) }

            targetFile
        } catch (e: Exception) {
            log.error { "Failed to download extension ${ext.id}: ${e.message}" }
            null
        }
    }

    suspend fun publishExtension(filePath: Path, metadata: ExtensionMetadata) = withContext(Dispatchers.IO) {
        try {
            val bytes = fs.source(filePath).buffered().use { it.readByteArray() }

            val payload = PublishPayload(
                metadata = json.encodeToString(metadata),
                fileBase64 = Base64.encode(bytes)
            )

            val response = httpClient.post(WORKER_PUBLISH_URL) {
                contentType(ContentType.Application.Json)
                bearerAuth(Base64.decode(KlyxBuildConfig.STORE_API_KEY).decodeToString())
                setBody(json.encodeToString(payload))
            }
            response.status.isSuccess().also { success ->
                if (!success) {
                    log.error { "Failed to publish '${metadata.name}': ${response.status}" }
                    val errorBody = response.bodyAsText()
                    log.error { errorBody }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun unpublishExtension(extensionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.delete("$WORKER_PUBLISH_URL?id=$extensionId") {
                bearerAuth(Base64.decode(KlyxBuildConfig.STORE_API_KEY).decodeToString())
            }

            response.status.isSuccess().also { success ->
                if (!success) {
                    val error = response.bodyAsText()
                    log.error { "Failed to unpublish '$extensionId': $error" }
                }
            }
        } catch (e: Exception) {
            log.error { "Network crash: ${e.message}" }
            false
        }
    }
}
