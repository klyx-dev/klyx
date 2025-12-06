package com.klyx.core.noderuntime

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.identity
import com.github.michaelbull.result.annotation.UnsafeResultErrorAccess
import com.github.michaelbull.result.flatten
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.zip
import com.klyx.core.AnyErr
import com.klyx.core.AnyResult
import com.klyx.core.Ok
import com.klyx.core.anyResult
import com.klyx.core.anyhow
import com.klyx.core.bail
import com.klyx.core.context
import com.klyx.core.file.archive.ArchiveType
import com.klyx.core.file.archive.extractGzipTar
import com.klyx.core.file.archive.extractZip
import com.klyx.core.file.downloadFile
import com.klyx.core.file.fs
import com.klyx.core.getOrNull
import com.klyx.core.io.Paths
import com.klyx.core.io.emptyPath
import com.klyx.core.io.intoPath
import com.klyx.core.io.join
import com.klyx.core.io.root
import com.klyx.core.io.stripSandboxRoot
import com.klyx.core.logging.Level
import com.klyx.core.logging.log
import com.klyx.core.logging.logErr
import com.klyx.core.logging.logger
import com.klyx.core.ok
import com.klyx.core.platform.Architecture
import com.klyx.core.platform.Os
import com.klyx.core.platform.currentOs
import com.klyx.core.platform.currentPlatform
import com.klyx.core.process.Command
import com.klyx.core.process.Output
import com.klyx.core.process.SystemPathSeparator
import com.klyx.core.process.getenv
import com.klyx.core.process.which
import com.klyx.core.unwrapOrDefault
import io.github.z4kn4fein.semver.Version
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import kotlinx.io.readString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.jvm.JvmSynthetic
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val NODE_CA_CERTS_ENV_VAR = "NODE_EXTRA_CA_CERTS"

@Serializable
data class NpmInfo(val distTags: NpmInfoDistTags, val versions: MutableList<String>)

@Serializable
data class NpmInfoDistTags(val latest: String?)

private data class NodeRuntimeState(
    val http: HttpClient,
    var instance: INodeRuntime?,
    var lastOptions: NodeBinaryOptions?,
    val options: StateFlow<NodeBinaryOptions?>,
    val shellEnvLoaded: CompletableDeferred<Unit>
)

@ConsistentCopyVisibility
data class NodeRuntime private constructor(private val state: NodeRuntimeState) {
    private val mutex = Mutex()

    private suspend inline fun <R> withState(block: (NodeRuntimeState) -> R): R = mutex.withLock { block(state) }

    suspend fun instance(): INodeRuntime = withState { state ->
        val options = run {
            var current = state.options.value
            while (current == null) {
                try {
                    state.options.first { it != null }
                    current = state.options.value
                } catch (error: Throwable) {
                    return UnavailableNodeRuntime(error = error.toString())
                }
            }
            current
        }

        if (state.lastOptions != options) {
            state.instance = null
        }

        state.instance?.let { return it.copy() }

        options.usePaths?.let { (node, npm) ->
            val instance = SystemNodeRuntime(node, npm)
                .fold(
                    success = { instance ->
                        log.info { "using Node.js from `node.path` in settings: $instance" }
                        instance
                    },
                    failure = { err ->
                        // failure case not cached, since it's cheap to check again
                        return UnavailableNodeRuntime(
                            error = "failure checking Node.js from `node.path` in settings ($node): $err"
                        )
                    }
                )
            state.instance = instance.copy()
            state.lastOptions = options
            return instance
        }

        val systemNodeError = if (options.allowPathLookup) {
            state.shellEnvLoaded.await()
            SystemNodeRuntime.detect()
                .fold(
                    success = { instance ->
                        log.info { "using Node.js found on PATH: $instance" }
                        state.instance = instance.copy()
                        state.lastOptions = options
                        return instance
                    },
                    ::identity
                )
        } else null

        val instance = if (options.allowBinaryDownload) {
            val (logLevel, whyUsingManaged) = when (systemNodeError) {
                is DetectError.Other -> Level.Warning to systemNodeError.error
                is DetectError.NotInPath -> Level.Info to systemNodeError.error
                null -> Level.Info to "`node.ignore_system_version` is `true` in settings"
            }

            ManagedNodeRuntime.installIfNeeded(state.http)
                .fold(
                    success = { instance ->
                        log.log(
                            logLevel,
                            "using Klyx managed Node.js at ${instance.installationPath} since $whyUsingManaged"
                        )
                        instance
                    },
                    failure = { err ->
                        // failure case is cached, since downloading + installing may be expensive. The
                        // downside of this is that it may fail due to an intermittent network issue.
                        //
                        // TODO: Have `install_if_needed` indicate which failure cases are retryable
                        // and/or have shared tracking of when internet is available.
                        UnavailableNodeRuntime(
                            "failure while downloading and/or installing Klyx managed Node.js, " +
                                    "restart Klyx to retry: $err"
                        )
                    }
                )
        } else if (systemNodeError != null) {
            // failure case not cached, since it's cheap to check again
            //
            // TODO: When support is added for setting `options.allow_binary_download`, update this
            // error message.
            UnavailableNodeRuntime("failure while checking system Node.js from PATH: $systemNodeError")
        } else {
            // failure case is cached because it will always happen with these options
            //
            // TODO: When support is added for setting `options.allow_binary_download`, update this
            // error message.
            UnavailableNodeRuntime("`node` settings do not allow any way to use Node.js")
        }

        state.instance = instance.copy()
        state.lastOptions = options
        instance
    }

    suspend fun binaryPath() = instance().binaryPath()

    suspend fun runNpmSubcommand(directory: Path, subcommand: String, args: Array<out String>): AnyResult<Output> {
        val instance = instance()
        return withState { (http) ->
            instance.runNpmSubcommand(directory, null, subcommand, args)
        }
    }

    suspend fun npmPackageInstalledVersion(localPackageDirectory: Path, name: String) =
        instance().npmPackageInstalledVersion(localPackageDirectory, name)

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun npmPackageLatestVersion(name: String): AnyResult<String> {
        val instance = instance()

        return withState { (http) ->
            anyhow {
                val output = instance.runNpmSubcommand(
                    directory = null,
                    proxy = null,
                    subcommand = "info",
                    args = arrayOf(
                        name,
                        "--json",
                        "--fetch-retry-mintimeout",
                        "2000",
                        "--fetch-retry-maxtimeout",
                        "5000",
                        "--fetch-timeout",
                        "5000",
                    )
                ).bind()

                val json = Json {
                    namingStrategy = JsonNamingStrategy.KebabCase
                    ignoreUnknownKeys = true
                }

                val info = json.decodeFromString(NpmInfo.serializer(), output.stdout)
                info.distTags.latest
                    ?: info.versions.removeLastOrNull()
                    ?: bail("no version found for npm package $name")
            }
        }
    }

    suspend fun npmInstallPackages(directory: Path, packages: Map<String, String>) = anyhow {
        if (packages.isEmpty()) return anyhow.ok(Unit)

        val packages = packages.map { (name, version) -> "$name@$version" }
        val arguments = packages.toMutableList()
        arguments.addAll(
            listOf(
                "--save-exact",
                "--fetch-retry-mintimeout",
                "2000",
                "--fetch-retry-maxtimeout",
                "5000",
                "--fetch-timeout",
                "5000",
            )
        )

        // This is also wrong because the directory is wrong.
        runNpmSubcommand(directory, "install", arguments.toTypedArray())
            .map { }
            .bind()
    }

    suspend fun shouldInstallNpmPackage(
        packageName: String,
        localExecutablePath: Path,
        localPackageDirectory: Path,
        versionStrategy: VersionStrategy
    ): Boolean {
        // In the case of the local system not having the package installed,
        // or in the instances where we fail to parse package.json data,
        // we attempt to install the package.
        if (fs.metadataOrNull(localExecutablePath) == null) return true

        val version = npmPackageInstalledVersion(localPackageDirectory, packageName)
            .logErr()
            .getOrNull() ?: return true

        val installedVersion = try {
            Version.parse(version)
        } catch (_: Throwable) {
            return true
        }

        return when (versionStrategy) {
            is VersionStrategy.Pin -> {
                val pinnedVersion = try {
                    Version.parse(versionStrategy.pinnedVersion)
                } catch (_: Throwable) {
                    return true
                }
                installedVersion != pinnedVersion
            }

            is VersionStrategy.Latest -> {
                val latestVersion = try {
                    Version.parse(versionStrategy.latestVersion)
                } catch (_: Throwable) {
                    return true
                }
                installedVersion < latestVersion
            }
        }
    }

    companion object {
        fun unavailable(): NodeRuntime {
            return NodeRuntime(
                NodeRuntimeState(
                    http = HttpClient(),
                    instance = null,
                    lastOptions = null,
                    options = MutableStateFlow(NodeBinaryOptions()),
                    shellEnvLoaded = CompletableDeferred()
                )
            )
        }

        fun newInstance(
            http: HttpClient,
            shellEnvLoaded: CompletableDeferred<Unit>?,
            options: StateFlow<NodeBinaryOptions?>
        ): NodeRuntime {
            return NodeRuntime(
                NodeRuntimeState(
                    http = http,
                    instance = null,
                    lastOptions = null,
                    options = options,
                    shellEnvLoaded = shellEnvLoaded ?: CompletableDeferred()
                )
            )
        }
    }
}

fun NodeRuntime(
    http: HttpClient,
    shellEnvLoaded: CompletableDeferred<Unit>?,
    options: StateFlow<NodeBinaryOptions?>
) = NodeRuntime.newInstance(http, shellEnvLoaded, options)

interface INodeRuntime {
    suspend fun copy(): INodeRuntime
    suspend fun binaryPath(): AnyResult<Path>

    suspend fun runNpmSubcommand(
        directory: Path?,
        proxy: Url?,
        subcommand: String,
        args: Array<out String>
    ): AnyResult<Output>

    suspend fun npmPackageInstalledVersion(
        localPackageDirectory: Path,
        name: String
    ): AnyResult<String?>
}

data class UnavailableNodeRuntime(private val error: String) : INodeRuntime {
    override suspend fun copy() = copy(error = error)
    override suspend fun binaryPath() = anyhow(error)

    override suspend fun runNpmSubcommand(
        directory: Path?,
        proxy: Url?,
        subcommand: String,
        args: Array<out String>
    ) = anyhow(error)

    override suspend fun npmPackageInstalledVersion(localPackageDirectory: Path, name: String) = anyhow(error)
}

@JvmSynthetic
private suspend fun pathWithNodeBinaryPrepended(nodeBinary: Path): Option<String> {
    val existingPath = getenv("PATH")
    val nodeBinDir = nodeBinary.parent

    return when {
        existingPath != null && nodeBinDir != null -> Some("$nodeBinDir$SystemPathSeparator$existingPath")
        existingPath != null -> Some(existingPath)
        nodeBinDir != null -> Some(nodeBinDir.toString())
        else -> None
    }
}

@JvmSynthetic
internal suspend fun configureNpmCommand(command: Command, directory: Path?, proxy: Url?) {
    directory?.let {
        command.currentDir(directory)
        command.args("--prefix".intoPath(), directory)
    }

    proxy?.let {
        val fixedProxy = proxy.forceLocalhostToIp()
        command.args("--proxy", fixedProxy.toString())
    }

    if (currentOs() == Os.Windows) {
        // SYSTEMROOT is a critical environment variables for Windows.
        getenv("SYSTEMROOT")?.let { v ->
            command.env("SYSTEMROOT", v)
        } ?: run {
            log.error { "Missing environment variable: SYSTEMROOT!" }
        }

        // Without ComSpec, the post-install will always fail.
        getenv("ComSpec")?.let { v ->
            command.env("ComSpec", v)
        } ?: run {
            log.error { "Missing environment variable: ComSpec!" }
        }
    }
}

fun Url.forceLocalhostToIp(): Url {
    if (!this.host.equals("localhost", ignoreCase = true)) return this

    // Map proxy settings from `http://localhost:10809` to `http://127.0.0.1:10809`
    // NodeRuntime without environment information can not parse `localhost`
    // correctly.
    // TODO: map to `[::1]` if we are using ipv6
    return URLBuilder(this).apply {
        // When localhost is a valid Host, so is `127.0.0.1`
        host = "127.0.0.1"
    }.build()
}

@JvmSynthetic
private suspend fun readPackageInstalledVersion(
    nodeModuleDirectory: Path,
    name: String
): AnyResult<String?> = anyhow {
    val packageJsonPath = nodeModuleDirectory.join(name, "package.json")

    val source = try {
        fs.source(packageJsonPath)
    } catch (_: FileNotFoundException) {
        return anyhow.ok(null)
    } catch (err: Throwable) {
        raise(err)
    }

    val contents = withContext(Dispatchers.IO) {
        source.buffered().use { it.readString() }
    }

    @Serializable
    data class PackageJson(val version: String)

    val json = Json { ignoreUnknownKeys = true }
    val packageJson: PackageJson = try {
        json.decodeFromString(contents)
    } catch (err: Throwable) {
        raise(err)
    }

    packageJson.version
}

@ConsistentCopyVisibility
private data class ManagedNodeRuntime private constructor(val installationPath: Path) : INodeRuntime {
    override suspend fun copy(): ManagedNodeRuntime {
        return copy(installationPath = installationPath)
    }

    override suspend fun binaryPath(): AnyResult<Path> {
        return Ok(installationPath.join(NODE_PATH))
    }

    @OptIn(UnsafeResultErrorAccess::class)
    override suspend fun runNpmSubcommand(
        directory: Path?,
        proxy: Url?,
        subcommand: String,
        args: Array<out String>
    ) = anyhow {
        val attempt = suspend {
            anyhow {
                val nodeBinary = installationPath.join(NODE_PATH)
                val npmFile = installationPath.join(NPM_PATH)
                val envPath = pathWithNodeBinaryPrepended(nodeBinary).unwrapOrDefault { "" }

                ensure(fs.exists(Paths.root.join(nodeBinary))) { "missing node binary file" }
                ensure(fs.exists(Paths.root.join(npmFile))) { "missing npm file" }

                val nodeCaCerts = getenv(NODE_CA_CERTS_ENV_VAR).orEmpty()

                val command = Command.newCommand(nodeBinary)
                    .env("PATH", envPath)
                    .env(NODE_CA_CERTS_ENV_VAR, nodeCaCerts)
                    .arg(npmFile).arg(subcommand)
                    .args("--cache".intoPath(), installationPath.join("cache"))
                    .args("--userconfig".intoPath(), installationPath.join("blank_user_npmrc"))
                    .args("--globalconfig".intoPath(), installationPath.join("blank_global_npmrc"))
                    .args(args = args)
                configureNpmCommand(command, directory, proxy)
                command.output(if (subcommand == "install") 5.minutes else 1.minutes).bind()
            }
        }

        var output = attempt()

        if (output.isErr) {
            output = attempt()
            ensure(output.isOk) { "failed to launch npm subcommand $subcommand subcommand\nerr: ${output.error}" }
        }

        output.onSuccess { output ->
            ensure(output.isSuccess) {
                "failed to execute npm $subcommand subcommand:\nstdout: ${output.stdout}\nstderr: ${output.stderr}"
            }
        }

        output.bind()
    }

    override suspend fun npmPackageInstalledVersion(
        localPackageDirectory: Path,
        name: String
    ) = readPackageInstalledVersion(localPackageDirectory.join("node_modules"), name)

    companion object {
        private val log = logger("ManagedNodeRuntime")

        private const val VERSION = "v24.11.0"
        private const val NODE_PATH = "bin/node"
        private const val NPM_PATH = "bin/npm"

        fun nodeContainingDirectory() = when (currentOs()) {
            Os.Android -> Paths.root.join("opt", "klyx", "node")
            else -> Paths.dataDir.join("node")
        }

        suspend fun installIfNeeded(http: HttpClient) = anyResult {
            val (os, arch) = currentPlatform()
            val osStr = when (os) {
                Os.Android, Os.Linux -> "linux"
                Os.Mac, Os.iOS -> "darwin"
                Os.Windows -> "win32"
                else -> bail("Running on unsupported os: $os")
            }

            val archStr = when (arch) {
                Architecture.Aarch64 -> "arm64"
                Architecture.X8664 -> "x64"
                else -> bail("Running on unsupported architecture: $arch")
            }

            val folderName = "node-$VERSION-$osStr-$archStr"
            val nodeContainingDir = when (os) {
                Os.Android -> Paths.root.join("opt", "klyx", "node")
                else -> Paths.dataDir.join("node")
            }

            val nodeDir = nodeContainingDir.join(folderName)
            val nodeBinary = nodeDir.join(NODE_PATH)
            val npmFile = nodeDir.join(NPM_PATH)
            val nodeCaCerts = getenv(NODE_CA_CERTS_ENV_VAR).orEmpty()

            val valid = fs.metadataOrNull(nodeBinary)?.let {
                val result = Command.newCommand(nodeBinary)
                    .clearEnv()
                    .env(NODE_CA_CERTS_ENV_VAR, nodeCaCerts)
                    .arg(npmFile.stripSandboxRoot())
                    .arg("--version")
                    .args("--cache".intoPath(), nodeDir.join("cache").stripSandboxRoot())
                    .args("--userconfig".intoPath(), nodeDir.join("blank_user_npmrc").stripSandboxRoot())
                    .args("--globalconfig".intoPath(), nodeDir.join("blank_global_npmrc").stripSandboxRoot())
                    .output(1.minutes)

                result.fold(
                    success = { output ->
                        if (output.isSuccess) {
                            true
                        } else {
                            log.warn {
                                "Klyx managed Node.js binary at $nodeBinary failed check with output: $output"
                            }
                            false
                        }
                    },
                    failure = { err ->
                        log.warn {
                            "Klyx managed Node.js binary at $nodeBinary failed check, so re-downloading it. Error: $err"
                        }
                        false
                    }
                )
            } ?: false

            if (!valid) {
                fs.delete(nodeContainingDir, mustExist = false)
                anyhow { fs.createDirectories(nodeContainingDir) }
                    .context("error creating node containing dir")
                    .bind()

                val archiveType = when (os) {
                    Os.Windows -> ArchiveType.Zip
                    Os.Mac, Os.Android, Os.Linux -> ArchiveType.TarGz
                    else -> bail("Running on unsupported os: $os")
                }

                val fileName = "node-$VERSION-$osStr-$archStr.$archiveType"

                val url = "https://nodejs.org/dist/$VERSION/$fileName"
                val downloadPath = nodeContainingDir.join("node.tar.gz")
                log.info { "Downloading Node.js binary from $url" }

                anyhow {
                    downloadFile(
                        url = url,
                        outputPath = downloadPath.toString(),
                        onComplete = { log.info { "Download of Node.js complete, extracting..." } },
                        onDownload = { sent, total ->
                            log.info { "[Node.js] Downloaded $sent bytes of $total" }
                        }
                    )
                }.context("error downloading Node binary tarball").bind()

                when (archiveType) {
                    ArchiveType.TarGz -> extractGzipTar(downloadPath, nodeContainingDir)
                    ArchiveType.Zip -> extractZip(downloadPath, nodeContainingDir)
                }
                log.info { "Extracted Node.js to $nodeContainingDir" }
                fs.delete(downloadPath, mustExist = false)
            }

            fs.createDirectories(nodeDir.join("cache"))
            fs.sink(nodeDir.join("blank_user_npmrc")).buffered().use { it.write(ByteArray(0)) }
            fs.sink(nodeDir.join("blank_global_npmrc")).buffered().use { it.write(ByteArray(0)) }

            ManagedNodeRuntime(installationPath = nodeDir.stripSandboxRoot())
        }
    }
}

private data class SystemNodeRuntime(
    val node: Path,
    val npm: Path,
    var globalNodeModules: Path,
    var scratchDir: Path
) : INodeRuntime {
    override suspend fun copy(): SystemNodeRuntime {
        return copy(node = node, npm = npm, globalNodeModules = globalNodeModules, scratchDir = scratchDir)
    }

    override suspend fun binaryPath() = Ok(node)

    override suspend fun runNpmSubcommand(
        directory: Path?,
        proxy: Url?,
        subcommand: String,
        args: Array<out String>
    ): AnyResult<Output> = anyhow {
        val nodeCaCerts = getenv(NODE_CA_CERTS_ENV_VAR).orEmpty()
        val command = Command.newCommand(npm)
        val path = pathWithNodeBinaryPrepended(node).unwrapOrDefault()
        command
            .env("PATH", path)
            .env(NODE_CA_CERTS_ENV_VAR, nodeCaCerts)
            .arg(subcommand)
            .args("--cache".intoPath(), scratchDir.join("cache"))
            .args(args = args)
        configureNpmCommand(command, directory, proxy)
        val output = command.output(1.minutes).bind()

        ensure(output.isSuccess) {
            "failed to execute npm $subcommand subcommand:\nstdout: ${output.stdout}\nstderr: ${output.stderr}"
        }
        output
    }

    override suspend fun npmPackageInstalledVersion(localPackageDirectory: Path, name: String) = run {
        readPackageInstalledVersion(localPackageDirectory.join("node_modules"), name)
        // TODO: allow returning a globally installed version (requires callers not to hard-code the path)
    }

    companion object {
        val MIN_VERSION = Version(22, 0, 0)

        suspend fun detect() = run {
            zip(
                { which("node").mapError(DetectError::NotInPath) },
                { which("npm").mapError(DetectError::NotInPath) }
            ) { node, npm ->
                SystemNodeRuntime(node, npm).mapError(DetectError::Other)
            }.flatten()
        }
    }
}

@Suppress("FunctionName")
@JvmSynthetic
private suspend fun SystemNodeRuntime(node: Path, npm: Path) = anyhow {
    val output = withContext("running node from $node") {
        Command.newCommand(node)
            .arg("--version")
            .output(30.seconds)
            .bind()
    }

    if (!output.isSuccess) {
        bail("failed to run node --version. stdout: ${output.stdout}, stderr: ${output.stderr}")
    }

    val versionStr = output.stdout
    val version = Version.parse(versionStr.trim().trimStart { it == 'v' })
    if (version < SystemNodeRuntime.MIN_VERSION) {
        bail("node at $node is too old. want: ${SystemNodeRuntime.MIN_VERSION}, got: $version")
    }

    val scratchDir = Paths.dataDir.join("node")
    anyhow { fs.createDirectories(scratchDir) }.ok()
    anyhow { fs.createDirectories(scratchDir.join("cache")) }.ok()

    val runtime = SystemNodeRuntime(node, npm, globalNodeModules = emptyPath(), scratchDir)
    runtime.runNpmSubcommand(null, null, "root", arrayOf("-g"))
        .bind()
        .let { runtime.globalNodeModules = Path(it.stdout) }
    runtime
}

private sealed interface DetectError {
    val error: Throwable

    data class NotInPath(override val error: AnyErr) : DetectError {
        override fun toString(): String {
            return "system Node.js wasn't found on PATH: $error"
        }
    }

    data class Other(override val error: AnyErr) : DetectError {
        override fun toString(): String {
            return "checking system Node.js failed with error: $error"
        }
    }
}
