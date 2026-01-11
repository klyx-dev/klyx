package com.klyx.extension

import arrow.core.raise.result
import com.klyx.core.file.downloadFile
import com.klyx.core.file.humanBytes
import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.io.extension
import com.klyx.core.io.isDirectory
import com.klyx.core.io.isFile
import com.klyx.core.io.okioFs
import com.klyx.core.logging.logger
import com.klyx.core.platform.Architecture
import com.klyx.core.platform.ExeSuffix
import com.klyx.core.platform.Os
import com.klyx.core.platform.currentOs
import com.klyx.core.platform.currentPlatform
import com.klyx.core.process.Command
import com.klyx.core.toSnakeCase
import com.klyx.extension.native.ParseExtensionVersionException
import com.klyx.extension.native.StripException
import com.klyx.extension.native.parseWasmExtensionVersion
import com.klyx.extension.native.stripCustomSections
import io.matthewnelson.kmp.process.Stdio
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.peanuuutz.tomlkt.Toml
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/// Currently, we compile with Rust's `wasm32-wasip2` target, which works with WASI `preview2` and the component model.
private const val RUST_TARGET = "wasm32-wasip2"

/**
 * Compiling Tree-sitter parsers from C to WASM requires Clang 17, and a WASM build of libc
 * and clang's runtime library. The `wasi-sdk` provides these binaries.
 *
 * Once Clang 17 and its wasm target are available via system package managers, we won't need
 * to download this.
 */
private const val WASI_SDK_URL = "https://github.com/WebAssembly/wasi-sdk/releases/download/wasi-sdk-25/"

private val WASI_SDK_ASSET_NAME by lazy {
    val (os, arch) = currentPlatform()

    when (os) {
        Os.Mac if arch == Architecture.X8664 -> "wasi-sdk-25.0-x86_64-macos.tar.gz"
        Os.Mac if arch == Architecture.Aarch64 -> "wasi-sdk-25.0-arm64-macos.tar.gz"
        Os.Linux if arch == Architecture.X8664 -> "wasi-sdk-25.0-x86_64-linux.tar.gz"
        Os.Linux if arch == Architecture.Aarch64 -> "wasi-sdk-25.0-arm64-linux.tar.gz"
        Os.Android if arch == Architecture.X8664 -> "wasi-sdk-25.0-x86_64-linux.tar.gz"
        Os.Android if arch == Architecture.Aarch64 -> "wasi-sdk-25.0-arm64-linux.tar.gz"
        Os.Windows if arch == Architecture.X8664 -> "wasi-sdk-25.0-x86_64-windows.tar.gz"
        else -> null
    }
}

@Serializable
data class CargoToml(
    @SerialName("package")
    val `package`: CargoTomlPackage
)

@Serializable
data class CargoTomlPackage(val name: String)

data class CompileExtensionOptions(val release: Boolean)

class ExtensionBuilder(val cacheDir: Path) {
    private val fs by lazy { okioFs }
    private val log = logger()

    suspend fun compileExtension(
        extensionDir: Path,
        manifest: ExtensionManifest,
        options: CompileExtensionOptions
    ) = result {
        populateDefaults(manifest, extensionDir)

        if (extensionDir.isRelative) {
            error("extension dir $extensionDir is not an absolute path")
        }

        fs.createDirectories(cacheDir)

        if (manifest.lib.kind == ExtensionLibraryKind.Rust) {
            log.info { "compiling Rust extension $extensionDir" }
            compileRustExtension(extensionDir, manifest, options)
            log.info { "compiled Rust extension $extensionDir" }
        }

        for ((grammarName, grammarMetadata) in manifest.grammars) {
            val snakeCasedGrammarName = grammarName.toSnakeCase()
            if (grammarName != snakeCasedGrammarName) {
                error("grammar name '$grammarName' must be written in snake_case: $snakeCasedGrammarName")
            }

            log.info { "compiling grammar $grammarName for extension $extensionDir" }
            compileGrammar(extensionDir, grammarName, grammarMetadata)
                .bind()
            log.info { "compiled grammar $grammarName for extension $extensionDir" }
        }

        log.info { "finished compiling extension $extensionDir" }
    }.getOrThrow()

    private suspend fun compileRustExtension(
        extensionDir: Path,
        manifest: ExtensionManifest,
        options: CompileExtensionOptions
    ) = result {
        installRustWasmTargetIfNeeded()

        val cargoTomlContent = fs.source(extensionDir / "Cargo.toml").buffer().use { it.readUtf8() }
        val cargoToml: CargoToml = Toml {
            ignoreUnknownKeys = true
        }.decodeFromString(cargoTomlContent)

        log.info { "compiling Rust crate for extension $extensionDir" }

        val output = Command.newCommand("cargo")
            .args("build", "--target", RUST_TARGET)
            .also { if (options.release) it.arg("--release") }
            .arg("--target-dir")
            .arg("${extensionDir / "target"}")
            // WASI builds do not work with sccache and just stuck, so disable it.
            .env("RUSTC_WRAPPER", "")
            .currentDir(extensionDir.toKotlinxIoPath())
            .output()
            .bind()

        if (!output.isSuccess) {
            raise(RuntimeException("failed to build extension ${output.stderr}"))
        }

        log.info { "compiled Rust crate for extension $extensionDir" }

        val wasmPath = (extensionDir / "target" / RUST_TARGET / (if (options.release) "release" else "debug"))
            .resolve(
                cargoToml
                    .`package`
                    .name
                    // The wasm32-wasip2 target normalizes `-` in package names to `_` in the resulting `.wasm` file.
                    .replace("-", "_") + ".wasm"
            )

        log.info { "encoding wasm component for extension $extensionDir" }

        var componentBytes = try {
            fs.source(wasmPath).buffer().use { it.readByteArray() }
        } catch (err: IOException) {
            throw IOException("failed to read output module $wasmPath: ${err.message}", err)
        }

        componentBytes = try {
            stripCustomSections(componentBytes)
        } catch (err: StripException) {
            throw RuntimeException("failed to strip debug sections from wasm component: $err", err)
        }

        val wasmExtensionApiVersion = try {
            parseWasmExtensionVersion(manifest.id, componentBytes)
        } catch (err: ParseExtensionVersionException) {
            throw RuntimeException("compiled wasm did not contain a valid klyx extension api version: $err", err)
        }
        manifest.lib.version = wasmExtensionApiVersion

        val extensionFile = extensionDir / "extension.wasm"
        try {
            fs.sink(extensionFile).buffer().use { it.write(componentBytes) }
        } catch (err: IOException) {
            raise(IOException("failed to write extension file $extensionFile: ${err.message}", err))
        }

        log.info { "extension $extensionDir written to $extensionFile" }
    }.getOrThrow()

    private suspend fun compileGrammar(
        extensionDir: Path,
        grammarName: String,
        grammarMetadata: GrammarManifestEntry
    ) = result {
        val clangPath = installWasiSdkIfNeeded()
        val grammarRepoDir = extensionDir / "grammars" / grammarName
        val grammarWasmPath = "$grammarRepoDir.wasm".toPath()

        log.info { "checking out $grammarName parser" }
        checkoutRepo(grammarRepoDir, grammarMetadata.repository, grammarMetadata.rev)

        val baseGrammarPath = grammarMetadata.path?.let { grammarRepoDir / it } ?: grammarRepoDir
        val srcPath = baseGrammarPath / "src"
        val parserPath = srcPath / "parser.c"
        val scannerPath = srcPath / "scanner.c"

        // Skip recompiling if the WASM object is already newer than the source files
        if (fileNewerThanDependencies(grammarWasmPath, listOf(parserPath, scannerPath))) {
            log.info {
                "skipping compilation of $grammarName parser because the existing compiled grammar is up to date"
            }
        } else {
            log.info { "compiling $grammarName parser" }

            val clangOutput = Command.newCommand(clangPath.toKotlinxIoPath())
                .args("-fPIC", "-shared", "-Os")
                .arg("-Wl,--export=tree_sitter_$grammarName")
                .arg("-o")
                .arg(grammarWasmPath.toString())
                .arg("-I")
                .arg(srcPath.toString())
                .arg(parserPath.toString())
                .also {
                    if (fs.exists(scannerPath)) it.arg(scannerPath.toString())
                }
                .output()
                .bind()

            if (!clangOutput.isSuccess) {
                raise(RuntimeException("failed to compile $grammarName parser with clang: ${clangOutput.stderr}"))
            }
        }
    }

    private suspend fun checkoutRepo(directory: Path, url: String, rev: String) = result {
        val gitDir = directory / ".git"

        if (fs.exists(gitDir)) {
            val remotesOutput = Command.newCommand("git")
                .arg("--git-dir")
                .arg(gitDir.toString())
                .args("remote", "-v")
                .output().bind()

            val hasRemote = remotesOutput.isSuccess && remotesOutput
                .stdout
                .lines()
                .any { line ->
                    val parts = line.split(Regex("\\s+"))
                    parts.first() == "origin" && parts.any { it == url }
                }

            if (!hasRemote) {
                raise(RuntimeException("grammar directory '$directory' already exists, but is not a git clone of '$url'"))
            }
        } else {
            runCatching { fs.createDirectories(directory) }
                .onFailure { raise(RuntimeException("failed to create grammar directory $directory: $it")) }

            val initOutput = Command.newCommand("git")
                .arg("init")
                .currentDir(directory.toKotlinxIoPath())
                .output()
                .bind()

            if (!initOutput.isSuccess) {
                raise(RuntimeException("failed to run `git init` in directory '$directory'"))
            }

            val remoteAddOutput = Command.newCommand("git")
                .arg("--git-dir")
                .arg(gitDir.toString())
                .args("remote", "add", "origin", url)
                .output()
                .bind()

            if (!remoteAddOutput.isSuccess) {
                raise(RuntimeException("failed to add remote $url for git repository $gitDir"))
            }

            val fetchOutput = Command.newCommand("git")
                .arg("--git-dir")
                .arg(gitDir.toString())
                .args("fetch", "--depth", "1", "origin", rev)
                .output()
                .bind()

            val checkoutOutput = Command.newCommand("git")
                .arg("--git-dir")
                .arg(gitDir.toString())
                .args("checkout", rev)
                .output()
                .bind()

            if (!checkoutOutput.isSuccess) {
                if (!fetchOutput.isSuccess) {
                    raise(RuntimeException("failed to fetch revision $rev in directory '$directory'"))
                }

                raise(RuntimeException("failed to checkout revision $rev in directory '$directory': ${checkoutOutput.stderr}"))
            }
        }
    }.getOrThrow()

    private suspend fun installRustWasmTargetIfNeeded() = result {
        val rustcOutput = Command.newCommand("rustc")
            .arg("--print")
            .arg("sysroot")
            .output()
            .bind()

        if (!rustcOutput.isSuccess) {
            raise(RuntimeException("failed to retrieve rust sysroot: ${rustcOutput.stderr}"))
        }

        val sysroot = rustcOutput.stdout.trim().toPath()
        if (fs.exists(sysroot / "lib" / "rustlib" / RUST_TARGET)) {
            return@result
        }

        val output = Command.newCommand("rustup")
            .args("target", "add", RUST_TARGET)
            .stderr(Stdio.Pipe)
            .stdout(Stdio.Inherit)
            .output()
            .bind()

        if (!output.isSuccess) {
            raise(RuntimeException("failed to install the `$RUST_TARGET` target: ${output.stderr}"))
        }
    }.getOrThrow()

    private suspend fun installWasiSdkIfNeeded() = result {
        val url = if (WASI_SDK_ASSET_NAME != null) {
            "$WASI_SDK_URL$WASI_SDK_ASSET_NAME"
        } else {
            raise(RuntimeException("wasi-sdk is not available for platform ${currentOs()}"))
        }

        val wasiSdkDir = cacheDir / "wasi-sdk"
        val clangPath = wasiSdkDir.resolve("bin/clang$ExeSuffix")

        log.info { "downloading wasi-sdk to $wasiSdkDir" }

        if (fs.metadata(clangPath).isRegularFile) {
            return@result clangPath
        }

        val tarOutDir = cacheDir / "wasi-sdk-temp"
        fs.deleteRecursively(wasiSdkDir)
        fs.deleteRecursively(tarOutDir)

        try {
            fs.createDirectories(tarOutDir, mustCreate = true)
        } catch (err: IOException) {
            raise(IOException("failed to create extraction directory: ${err.message}", err))
        }

        val tarGzPath = "$cacheDir/wasi-sdk.tar.gz"

        downloadFile(
            url = url,
            outputPath = tarGzPath,
            onDownload = { sent, total ->
                log.progress { "[wasi-sdk.tar.gz] downloaded ${sent.humanBytes()} of ${total?.humanBytes()}" }
            }
        )

        log.info { "un-tarring wasi-sdk to $tarOutDir" }

        val tarOutput = Command.newCommand("tar")
            .args("-xzf", tarGzPath, "-C", tarOutDir.toString())
            .output()
            .bind()

        if (!tarOutput.isSuccess) {
            raise(RuntimeException("failed to extract wasi-sdk archive: ${tarOutput.stderr}"))
        }

        log.info { "finished downloading wasi-sdk" }

        // Clean up the temporary tar.gz file
        fs.delete(tarGzPath.toPath())

        val innerDir = result {
            fs.list(tarOutDir).firstOrNull() ?: raise(RuntimeException("no content"))
        }.bind()

        fs.atomicMove(innerDir, wasiSdkDir)
        fs.deleteRecursively(tarOutDir)
        clangPath
    }.getOrThrow()
}

private suspend fun populateDefaults(manifest: ExtensionManifest, extensionPath: Path) {
    val fs by lazy { okioFs }

    val cargoTomlPath = extensionPath / "Cargo.toml"
    if (fs.exists(cargoTomlPath)) {
        manifest.lib.kind = ExtensionLibraryKind.Rust
    }

    val languagesDir = extensionPath / "languages"
    if (fs.isDirectory(languagesDir)) {
        val languageDirEntries = fs.list(languagesDir)

        for (languageDir in languageDirEntries) {
            val configPath = languageDir / "config.toml"
            if (fs.isFile(configPath)) {
                val relativeLanguageDir = languageDir.relativeTo(extensionPath)
                if (!manifest.languages.contains(relativeLanguageDir)) {
                    manifest.languages += relativeLanguageDir
                }
            }
        }
    }

    val themesDir = extensionPath / "themes"
    if (fs.isFile(themesDir)) {
        val themeDirEntries = fs.list(themesDir)

        for (themePath in themeDirEntries) {
            if (themePath.extension() == "json") {
                val relativeThemePath = themePath.relativeTo(extensionPath)
                if (!manifest.themes.contains(relativeThemePath)) {
                    manifest.themes += relativeThemePath
                }
            }
        }
    }

    val iconThemesDir = extensionPath / "icon_themes"
    if (fs.isDirectory(iconThemesDir)) {
        val iconThemeDirEntries = fs.list(iconThemesDir)

        for (iconThemePath in iconThemeDirEntries) {
            if (iconThemePath.extension() == "json") {
                val relativeIconThemePath = iconThemePath.relativeTo(extensionPath)
                if (!manifest.iconThemes.contains(relativeIconThemePath)) {
                    manifest.iconThemes += relativeIconThemePath
                }
            }
        }
    }

    if (manifest.snippets == null && fs.isFile(extensionPath / "snippets.json")) {
        manifest.snippets = "snippets.json".toPath()
    }
}

/**
 * Returns `true` if the [target] exists and its last modified time is greater than that
 * of each dependency which exists (i.e., dependency paths which do not exist are ignored).
 */
@Throws(IOException::class, IllegalStateException::class)
private fun fileNewerThanDependencies(target: Path, dependencies: List<Path>): Boolean {
    if (okioFs.exists(target)) return false
    val targetModified = okioFs.metadata(target).lastModifiedAtMillis ?: error("no last modified time")

    for (dependency in dependencies) {
        if (!okioFs.exists(dependency)) continue

        val depModified = okioFs.metadata(dependency).lastModifiedAtMillis ?: error("no last modified time")
        if (depModified > targetModified) return true
    }
    return true
}
