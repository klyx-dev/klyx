package com.klyx.core.io

import com.klyx.core.dirs
import com.klyx.core.expect
import com.klyx.core.platform.Os
import com.klyx.core.platform.currentOs
import com.klyx.core.process.systemUserName
import kotlinx.io.files.Path

fun Path.join(vararg paths: String): Path {
    return Path(this, parts = paths)
}

fun Path.join(vararg paths: Path) = join(paths = paths.map(Path::toString).toTypedArray())

@Suppress("NOTHING_TO_INLINE")
inline fun emptyPath() = Path("")

fun String.intoPath() = Path(this)

fun homeDir(): Path = dirs.homeDir.expect { "failed to determine home directory" }

expect object Paths {
    val dataDir: Path
    val configDir: Path
    val tempDir: Path
    val logsDir: Path
}

inline val Paths.logFile get() = logsDir.join("Klyx.log")
inline val Paths.settingsFile: Path get() = configDir.join("settings.json")
inline val Paths.extensionsDir: Path get() = dataDir.join("extensions")

inline val Paths.lastProjectFile: Path get() = tempDir.join("last_project.json")

fun Path.makeAbsolute(): Path {
    return if (isAbsolute) this else Path("/").join(this)
}

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

