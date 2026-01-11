package com.klyx.extension.host

import com.klyx.core.Notifier
import com.klyx.core.io.getReleaseByTagName
import com.klyx.core.noderuntime.NodeRuntime
import com.klyx.core.notification.Toast
import com.klyx.core.process.systemProcess
import com.klyx.core.util.intoPath
import com.klyx.extension.ExtensionHostProxy
import com.klyx.extension.SchemaVersion
import com.klyx.extension.native.Command
import com.klyx.extension.native.ExtensionRuntimeException
import com.klyx.extension.native.GithubRelease
import com.klyx.extension.native.GithubReleaseAsset
import com.klyx.extension.native.HttpMethod
import com.klyx.extension.native.HttpRequest
import com.klyx.extension.native.HttpResponse
import com.klyx.extension.native.HttpResponseException
import com.klyx.extension.native.HttpResponseStream
import com.klyx.extension.native.NodeRuntimeException
import com.klyx.extension.native.Output
import com.klyx.extension.native.RedirectPolicy
import com.klyx.extension.native.ToastDuration
import com.klyx.extension.native.WasmExtensionGithubHost
import com.klyx.extension.native.WasmExtensionHost
import com.klyx.extension.native.WasmExtensionHttpClientHost
import com.klyx.extension.native.WasmExtensionNodeRuntimeHost
import com.klyx.extension.native.WasmExtensionProcessHost
import com.klyx.extension.native.WasmExtensionSystemHost
import com.klyx.extension.native.WasmHost
import com.klyx.util.getOrThrow
import io.itsvks.anyhow.getOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The current extension [SchemaVersion] supported by Klyx.
 */
val CurrentSchemaVersion = SchemaVersion(1)

/**
 * Returns the [SchemaVersion] range that is compatible with this version of Klyx.
 */
fun schemaVersionRange() = SchemaVersion.Zero..CurrentSchemaVersion

fun WasmHost(workdir: String, proxy: ExtensionHostProxy, nodeRuntime: NodeRuntime): WasmHost {
    return WasmHost(
        workDir = workdir,
        extensionHost = WasmExtensionHost(nodeRuntime),
        imports = ExtensionImports(proxy)
    )
}

fun WasmExtensionHost(nodeRuntime: NodeRuntime) = WasmExtensionHost(
    process = ProcessHost,
    nodeRuntime = NodeRuntimeHost(nodeRuntime),
    system = SystemHost,
    httpClient = HttpClientHost,
    github = GitHubHost
)

private object GitHubHost : WasmExtensionGithubHost {
    override suspend fun latestGithubRelease(
        repo: String,
        requireAssets: Boolean,
        preRelease: Boolean
    ): GithubRelease {
        return try {
            com.klyx.core.io.latestGithubRelease(repo, requireAssets, preRelease).native()
        } catch (t: Throwable) {
            throw ExtensionRuntimeException(t.stackTraceToString())
        }
    }

    override suspend fun githubReleaseByTagName(
        repo: String,
        tag: String
    ): GithubRelease {
        return try {
            getReleaseByTagName(repo, tag).native()
        } catch (t: Throwable) {
            throw ExtensionRuntimeException(t.stackTraceToString())
        }
    }

    private fun com.klyx.core.io.GithubRelease.native() = GithubRelease(
        tagName = tagName,
        preRelease = preRelease,
        assets = assets.map { (name, browserDownloadUrl, digest) ->
            GithubReleaseAsset(
                name = name,
                browserDownloadUrl = browserDownloadUrl,
                digest = digest
            )
        },
        tarballUrl = tarballUrl,
        zipballUrl = zipballUrl
    )
}

private object HttpClientHost : WasmExtensionHttpClientHost {
    private val client = HttpClient {
        followRedirects = true
    }

    private val noFollowClient = HttpClient {
        followRedirects = false
    }

    override suspend fun fetch(request: HttpRequest): HttpResponse {
        return try {
            fetchResponse(request).native()
        } catch (t: Throwable) {
            throw HttpResponseException(t.stackTraceToString())
        }
    }

    override fun fetchStream(request: HttpRequest): HttpResponseStream = runBlocking {
        withContext(Dispatchers.IO) {
            val response = try {
                fetchResponse(request)
            } catch (t: Throwable) {
                throw HttpResponseException(t.stackTraceToString())
            }

            ResponseStream(response.bodyAsChannel())
        }
    }

    private suspend fun fetchResponse(request: HttpRequest): io.ktor.client.statement.HttpResponse {
        return when (request.redirectPolicy) {
            RedirectPolicy.FollowAll -> client.request(request.url) {
                method = when (request.method) {
                    HttpMethod.GET -> io.ktor.http.HttpMethod.Get
                    HttpMethod.HEAD -> io.ktor.http.HttpMethod.Head
                    HttpMethod.POST -> io.ktor.http.HttpMethod.Post
                    HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
                    HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
                    HttpMethod.OPTIONS -> io.ktor.http.HttpMethod.Options
                    HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
                }
                headers { request.headers.forEach { (k, v) -> append(k, v) } }
                request.body?.let { setBody(it) }
            }

            is RedirectPolicy.FollowLimit -> {
                var currentUrl = request.url
                var redirectCount = 0
                var finalResponse: io.ktor.client.statement.HttpResponse

                while (true) {
                    val response = client.request(currentUrl) {
                        method = when (request.method) {
                            HttpMethod.GET -> io.ktor.http.HttpMethod.Get
                            HttpMethod.HEAD -> io.ktor.http.HttpMethod.Head
                            HttpMethod.POST -> io.ktor.http.HttpMethod.Post
                            HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
                            HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
                            HttpMethod.OPTIONS -> io.ktor.http.HttpMethod.Options
                            HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
                        }
                        headers { request.headers.forEach { (k, v) -> append(k, v) } }
                        request.body?.let { setBody(it) }
                    }

                    if (response.status.isRedirect()) {
                        val location = response.headers[HttpHeaders.Location] ?: error("Redirect without Location")
                        redirectCount++
                        val policy = request.redirectPolicy as RedirectPolicy.FollowLimit
                        if (redirectCount > policy.v1.toInt()) error("Too many redirects")
                        currentUrl = location
                    } else {
                        finalResponse = response
                        break
                    }
                }

                finalResponse
            }

            RedirectPolicy.NoFollow -> noFollowClient.request(request.url) {
                method = when (request.method) {
                    HttpMethod.GET -> io.ktor.http.HttpMethod.Get
                    HttpMethod.HEAD -> io.ktor.http.HttpMethod.Head
                    HttpMethod.POST -> io.ktor.http.HttpMethod.Post
                    HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
                    HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
                    HttpMethod.OPTIONS -> io.ktor.http.HttpMethod.Options
                    HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
                }
                headers { request.headers.forEach { (k, v) -> append(k, v) } }
                request.body?.let { setBody(it) }
                expectSuccess = true
            }
        }
    }

    private fun HttpStatusCode.isRedirect() = value in 300..399

    private suspend fun io.ktor.client.statement.HttpResponse.native() = HttpResponse(
        headers = headers
            .entries()
            .associateBy(
                keySelector = { it.key },
                valueTransform = { it.value.joinToString(",") }
            ),
        body = bodyAsBytes()
    )

    private class ResponseStream(private val channel: ByteReadChannel) : HttpResponseStream {
        companion object {
            private const val MAX_CHUNK_SIZE = 16_384
        }

        private var finished = atomic(false)

        override suspend fun nextChunk(): ByteArray? {
            if (finished.value) return null

            val buffer = ByteArray(MAX_CHUNK_SIZE)
            val bytesRead = channel.readAvailable(buffer)
            return if (bytesRead == -1) {
                finished.compareAndSet(expect = false, update = true)
                null
            } else {
                buffer.copyOf(bytesRead)
            }
        }
    }
}

private object SystemHost : WasmExtensionSystemHost, KoinComponent {
    private val notifier: Notifier by inject()

    override suspend fun showToast(message: String, duration: ToastDuration) {
        withContext(Dispatchers.Main) {
            val durationMillis = when (duration) {
                ToastDuration.SHORT -> Toast.LENGTH_SHORT
                ToastDuration.LONG -> Toast.LENGTH_LONG
            }
            notifier.toast(message, durationMillis)
        }
    }
}

private class NodeRuntimeHost(private val nodeRuntime: NodeRuntime) : WasmExtensionNodeRuntimeHost {
    override suspend fun nodeBinaryPath(): String {
        return nodeRuntime.binaryPath().getOrThrow { NodeRuntimeException(it.stackTraceToString()) }.toString()
    }

    override suspend fun npmPackageLatestVersion(name: String): String {
        return nodeRuntime.npmPackageLatestVersion(name).getOrThrow { NodeRuntimeException(it.stackTraceToString()) }
    }

    override suspend fun npmPackageInstalledVersion(
        localPackageDirectory: String,
        name: String
    ): String? {
        return nodeRuntime.npmPackageInstalledVersion(localPackageDirectory.intoPath(), name)
            .getOrThrow { NodeRuntimeException(it.stackTraceToString()) }
    }

    override suspend fun npmInstallPackages(
        directory: String,
        packages: Map<String, String>
    ) {
        nodeRuntime.npmInstallPackages(directory.intoPath(), packages)
            .getOrThrow { NodeRuntimeException(it.stackTraceToString()) }
    }
}

private object ProcessHost : WasmExtensionProcessHost {
    override suspend fun runCommand(command: Command): Output {
        return systemProcess(command.command, *command.args.toTypedArray()) {
            environment { putAll(command.env) }
        }.output().let {
            Output(
                status = it.processInfo.exitCode,
                stdout = it.stdout.encodeToByteArray(),
                stderr = it.stderr.encodeToByteArray()
            )
        }
    }
}
