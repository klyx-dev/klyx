package com.klyx.core

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.some
import com.klyx.core.util.join
import com.klyx.core.platform.selectByOs
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

@Suppress("ClassName")
actual object dirs {

    private fun pathOf(env: String?) = env?.let(::Path)
    private fun xdg(name: String) = pathOf(System.getenv(name))

    private val resolvedHome: String by lazy {
        System.getenv("HOME")
            ?: System.getProperty("user.home")
            ?: System.getenv("USERPROFILE")
            ?: error("unable to determine HOME directory")
    }

    private val xdgConfigHome: Path by lazy {
        System.getenv("XDG_CONFIG_HOME")?.let(::Path)
            ?: Path(resolvedHome, ".config")
    }

    private val userDirFilePath: Path by lazy {
        xdgConfigHome.join("user-dirs.dirs")
    }

    private fun readFileOrEmpty(path: Path): String =
        try {
            SystemFileSystem.source(path).buffered().readString()
        } catch (_: Throwable) {
            ""
        }

    private fun String.trimBlank(): String =
        this.trim { it == ' ' || it == '\t' }

    private fun shellUnescapeString(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                out.append(s[i + 1])
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun parseUserDirs(
        homeDir: Path,
        only: String? // null = all dirs
    ): Map<String, Path> {

        val text = readFileOrEmpty(userDirFilePath)
        if (text.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, Path>()

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trimBlank()
            if (line.isEmpty() || line.startsWith("#")) continue

            val idx = line.indexOf('=')
            if (idx == -1) continue

            val keyRaw = line.take(idx).trimBlank()
            val valRaw = line.substring(idx + 1).trimBlank()

            // Must be XDG_<NAME>_DIR=
            if (!keyRaw.startsWith("XDG_") || !keyRaw.endsWith("_DIR")) continue

            val key = keyRaw.removePrefix("XDG_").removeSuffix("_DIR")
            if (only != null && key != only) continue

            if (!(valRaw.startsWith("\"") && valRaw.endsWith("\""))) continue

            var value = valRaw.substring(1, valRaw.length - 1)

            if (value == $$"$HOME/") continue // disabled dir

            val isRelative: Boolean
            when {
                value.startsWith($$"$HOME/") -> {
                    value = value.removePrefix($$"$HOME/")
                    isRelative = true
                }

                value.startsWith("/") -> {
                    isRelative = false
                }

                else -> continue // invalid
            }

            val unescaped = shellUnescapeString(value)
            val path = if (isRelative)
                homeDir.join(unescaped)
            else Path(unescaped)

            result[key] = path

            if (only != null) break
        }

        return result
    }

    private fun xdgUserDir(name: String) = parseUserDirs(Path(resolvedHome), only = name)[name].orNone()

    actual val homeDir: Option<Path> by lazy {
        Some(Path(resolvedHome))
    }

    actual val cacheDir by lazy {
        selectByOs(
            windows = {
                // LOCALAPPDATA -> APPDATA -> HOME\AppData\Local
                pathOf(System.getenv("LOCALAPPDATA"))?.some()
                    ?: pathOf(System.getenv("APPDATA"))?.some()
                    ?: homeDir.map { it.join("AppData", "Local") }
            },
            mac = {
                homeDir.map { it.join("Library", "Caches") }
            },
            linux = {
                xdg("XDG_CACHE_HOME")?.some()
                    ?: homeDir.map { it.join(".cache") }
            }
        )
    }

    actual val configDir: Option<Path> by lazy {
        selectByOs(
            windows = {
                pathOf(System.getenv("APPDATA"))?.some()
                    ?: homeDir.map { it.join("AppData", "Roaming") }
            },
            mac = {
                homeDir.map { it.join("Library", "Application Support") }
            },
            linux = {
                xdg("XDG_CONFIG_HOME")?.some()
                    ?: homeDir.map { it.join(".config") }
            }
        )
    }

    actual val configLocalDir: Option<Path> by lazy {
        selectByOs(
            windows = {
                pathOf(System.getenv("LOCALAPPDATA"))?.some()
                    ?: homeDir.map { it.join("AppData", "Local") }
            },
            mac = { homeDir.map { it.join("Library", "Application Support") } },
            linux = { xdg("XDG_CONFIG_HOME")?.some() ?: homeDir.map { it.join(".config") } }
        )
    }

    actual val dataDir: Option<Path> by lazy {
        selectByOs(
            windows = {
                pathOf(System.getenv("APPDATA"))?.some()
                    ?: homeDir.map { it.join("AppData", "Roaming") }
            },
            mac = { homeDir.map { it.join("Library", "Application Support") } },
            linux = {
                xdg("XDG_DATA_HOME")?.some()
                    ?: homeDir.map { it.join(".local", "share") }
            }
        )
    }

    actual val dataLocalDir: Option<Path> by lazy {
        selectByOs(
            windows = {
                pathOf(System.getenv("LOCALAPPDATA"))?.some()
                    ?: homeDir.map { it.join("AppData", "Local") }
            },
            mac = {
                homeDir.map { it.join("Library", "Application Support") }
            },
            linux = {
                xdg("XDG_DATA_HOME")?.some()
                    ?: homeDir.map { it.join(".local", "share") }
            }
        )
    }

    actual val executableDir: Option<Path> by lazy {
        selectByOs(
            windows = { None },
            mac = { None },
            linux = {
                xdg("XDG_BIN_HOME")?.some() ?: homeDir.map { it.join(".local", "bin") }
            }
        )
    }

    actual val preferenceDir: Option<Path> by lazy {
        selectByOs(
            windows = {
                pathOf(System.getenv("APPDATA"))?.some()
                    ?: homeDir.map { it.join("AppData", "Roaming") }
            },
            mac = { homeDir.map { it.join("Library", "Preferences") } },
            linux = {
                xdg("XDG_CONFIG_HOME")?.some() ?: homeDir.map { it.join(".config") }
            }
        )
    }

    actual val runtimeDir: Option<Path> by lazy {
        selectByOs(
            windows = { None },
            mac = { None },
            linux = { xdg("XDG_RUNTIME_DIR").orNone() }
        )
    }

    actual val stateDir: Option<Path> by lazy {
        selectByOs(
            windows = { None },
            mac = { None },
            linux = {
                xdg("XDG_STATE_HOME")?.some() ?: homeDir.map { it.join(".local", "state") }
            }
        )
    }

    actual val audioDir: Option<Path> by lazy {
        selectByOs(
            windows = { homeDir.map { it.join("Music") } },
            mac = { homeDir.map { it.join("Music") } },
            linux = { xdgUserDir("MUSIC") }
        )
    }

    actual val desktopDir: Option<Path> by lazy {
        selectByOs(
            windows = { homeDir.map { it.join("Desktop") } },
            mac = { homeDir.map { it.join("Desktop") } },
            linux = { xdgUserDir("DESKTOP") }
        )
    }

    actual val documentDir: Option<Path> by lazy {
        selectByOs(
            windows = { homeDir.map { it.join("Documents") } },
            mac = { homeDir.map { it.join("Documents") } },
            linux = { xdgUserDir("DOCUMENTS") }
        )
    }

    actual val downloadDir: Option<Path> by lazy {
        selectByOs(
            windows = { homeDir.map { it.join("Downloads") } },
            mac = { homeDir.map { it.join("Downloads") } },
            linux = { xdgUserDir("DOWNLOAD") }
        )
    }

    actual val pictureDir: Option<Path> by lazy {
        selectByOs(
            windows = { homeDir.map { it.join("Pictures") } },
            mac = { homeDir.map { it.join("Pictures") } },
            linux = { xdgUserDir("PICTURES") }
        )
    }

    actual val publicDir: Option<Path> by lazy {
        selectByOs(
            windows = { homeDir.map { it.join("Public") } },
            mac = { homeDir.map { it.join("Public") } },
            linux = { xdgUserDir("PUBLICSHARE") }
        )
    }

    actual val templateDir: Option<Path> by lazy {
        selectByOs(
            windows = { homeDir.map { it.join("Templates") } },
            mac = { None },
            linux = { xdgUserDir("TEMPLATES") }
        )
    }

    actual val videoDir: Option<Path> by lazy {
        selectByOs(
            windows = { homeDir.map { it.join("Videos") } },
            mac = { homeDir.map { it.join("Movies") } },
            linux = { xdgUserDir("VIDEOS") }
        )
    }

    actual val fontDir: Option<Path> by lazy {
        selectByOs(
            windows = { None },
            mac = { homeDir.map { it.join("Library", "Fonts") } },
            linux = {
                xdg("XDG_DATA_HOME")?.let {
                    Path(it).join("fonts").some()
                } ?: homeDir.map { it.join(".local", "share", "fonts") }
            }
        )
    }
}
