package com.klyx.core.io

import com.klyx.core.dirs
import com.klyx.core.expect
import com.klyx.core.platfrom.Os
import com.klyx.core.platfrom.currentOs
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

fun Path.makeAbsolute(): Path {
    return if (isAbsolute) this else Path("/").join(this)
}

fun Path.resolveForAndroid(): Path {
    return when (currentOs()) {
        Os.Android -> {
            val path = this.toString()
            if (path.startsWith(Paths.root.toString())) {
                path.removePrefix(Paths.root.toString()).intoPath()
            } else {
                Path(path)
            }
        }

        else -> this
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

