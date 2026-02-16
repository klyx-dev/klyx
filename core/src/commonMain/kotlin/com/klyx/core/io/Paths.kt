package com.klyx.core.io

import com.klyx.core.dirs
import com.klyx.core.expect
import com.klyx.core.platform.Os
import com.klyx.core.platform.currentOs
import com.klyx.core.process.systemUserName
import com.klyx.core.util.intoPath
import com.klyx.core.util.join
import kotlinx.io.files.Path

fun homeDir(): Path = dirs.homeDir.expect { "failed to determine home directory" }

expect object Paths {
    val dataDir: Path
    val configDir: Path
    val tempDir: Path
    val logsDir: Path
}

expect val Paths.androidExternalFilesDir: Path
expect val Paths.androidNativeLibraryDir: Path

inline val Paths.logFile get() = logsDir.join("Klyx.log")
inline val Paths.oldLogFile get() = logsDir.join("Klyx.log.old")

inline val Paths.settingsFile: Path get() = configDir.join("settings.json")
inline val Paths.globalSettingsFile: Path get() = configDir.join("global_settings.json")
inline val Paths.settingsBackupFile: Path get() = configDir.join("settings_backup.json")

inline val Paths.extensionsDir: Path get() = dataDir.join("extensions")

/**
 * Returns the path to the languages directory.
 *
 * This is where language servers are downloaded to for languages built-in to Zed.
 */
inline val Paths.languagesDir: Path get() = dataDir.join("languages")

inline val Paths.lastProjectFile: Path get() = tempDir.join("last_project.json")

@Suppress("NOTHING_TO_INLINE")
inline fun String.stripSandboxRoot(): String = Path(this).stripSandboxRoot().toString()

fun Path.stripSandboxRoot(): Path {
    if (currentOs() != Os.Android) return this

    val root = Paths.root.toString()
    val str = this.toString()

    return when {
        str.startsWith(root) -> str.removePrefix(root).intoPath()
        else -> this
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.resolveToSandbox(): String = Path(this).resolveToSandbox().toString()

fun Path.resolveToSandbox(): Path {
    if (currentOs() != Os.Android) return this

    val root = Paths.root
    val str = this.toString()

    return when {
        !this.isAbsolute -> root.join(str)
        str.startsWith(root.toString()) -> this
        else -> this // error("Path is outside sandbox")
    }
}

val Paths.homeDir
    get() = when (currentOs()) {
        Os.Android -> dataDir.join("sandbox", "home", systemUserName)
        else -> homeDir()
    }

val Paths.root
    get() = when (currentOs()) {
        Os.Android -> dataDir.join("sandbox")
        else -> Path("/")
    }

/**
 * Returns the relative path to a `.klyx` folder within a project.
 */
val Paths.localSettingsFolderName get() = ".klyx"

/**
 * Returns the relative path to a `.vscode` folder within a project.
 */
val Paths.localVSCodeFolderName get() = ".vscode"

val Paths.taskFileName get() = "tasks.json"

/**
 * Returns the relative path to a `settings.json` file within a project.
 */
val Paths.localSettingsFileRelativePath by lazy {
    Path(".klyx/settings.json")
}

/**
 * Returns the relative path to a `tasks.json` file within a project.
 */
val Paths.localTasksFileRelativePath by lazy {
    Path(".klyx/tasks.json")
}

/**
 * Returns the relative path to a `.vscode/tasks.json` file within a project.
 */
val Paths.localVSCodeTasksFileRelativePath by lazy {
    Path(".vscode/tasks.json")
}

fun okio.Path.extension() = toString().substringAfterLast('.')
fun Path.extension() = toString().substringAfterLast('.')
