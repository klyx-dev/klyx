package com.klyx.core.io

import com.klyx.core.dirs
import com.klyx.core.expect
import com.klyx.core.platform.selectByOs
import com.klyx.core.util.join
import kotlinx.io.files.Path

actual object Paths {
    actual val dataDir by lazy {
        selectByOs(
            windows = {
                dirs.dataLocalDir
                    .expect { "failed to determine LocalAppData directory" }
                    .join("Klyx")
            },
            linux = {
                (System.getenv("FLATPAK_XDG_DATA_HOME")?.let(::Path)
                    ?: dirs.dataLocalDir.expect { "failed to determine XDG_DATA_HOME directory" }).join("klyx")
            },
            mac = { homeDir().join("Library/Application Support/Klyx") },
            default = { configDir }
        )
    }

    actual val configDir: Path by lazy {
        selectByOs(
            windows = {
                dirs.configDir
                    .expect { "failed to determine RoamingAppData directory" }
                    .join("Klyx")
            },
            linux = {
                (System.getenv("FLATPAK_XDG_CONFIG_HOME")
                    ?.let(::Path)
                    ?: dirs.configDir.expect { "failed to determine XDG_CONFIG_HOME directory" })
                    .join("klyx")
            },
            default = { homeDir().join(".config", "klyx") }
        )
    }

    actual val tempDir: Path by lazy {
        selectByOs(
            mac = {
                dirs.cacheDir
                    .expect { "failed to determine cachesDirectory directory" }
                    .join("Klyx")
            },
            windows = {
                dirs.cacheDir
                    .expect { "failed to determine LocalAppData directory" }
                    .join("Klyx")
            },
            linux = {
                (System.getenv("FLATPAK_XDG_CACHE_HOME")
                    ?.let(::Path)
                    ?: dirs.cacheDir.expect { "failed to determine XDG_CACHE_HOME directory" })
                    .join("klyx")
            },
            default = { homeDir().join(".cache").join("klyx") }
        )
    }

    actual val logsDir: Path by lazy {
        selectByOs(
            mac = { homeDir().join("Library/Logs/Klyx") },
            default = { dataDir.join("logs") }
        )
    }
}
