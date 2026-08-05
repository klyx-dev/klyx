package com.klyx.data.runner

import com.klyx.api.data.fs.Paths
import com.klyx.api.data.runner.FileRunRequest
import com.klyx.api.data.runner.FileRunner
import com.klyx.api.data.runner.FileRunnerContext
import com.klyx.api.terminal.home
import java.io.File

/**
 * Built-in [FileRunner] that executes a Python file in the integrated terminal.
 *
 * when a `.py` file is active, the editor shows the "Run" button and clicking it opens the terminal and runs
 * `python <file>`.
 */
class PythonFileRunner : FileRunner {

    override val id = "com.klyx.runner.python"

    override val priority = 10

    override fun supports(request: FileRunRequest): Boolean =
        request.extension == "py"

    override suspend fun run(request: FileRunRequest, runner: FileRunnerContext) {
        val realPath = request.uri.path ?: return
        val containerPath = toContainerPath(realPath)
        val dir = containerPath.substringBeforeLast('/', missingDelimiterValue = "")

        runner.runInTerminal(
            command = "python3 ${shellQuote(containerPath)}",
            cwd = dir.takeIf { it.isNotBlank() },
            sessionName = request.file.name,
        )
    }

    /**
     * Translates a real filesystem path into the path visible inside the terminal's PRoot
     * environment.
     *
     * The real path is canonicalized first because the URI may point through a symlink (for
     * example `/data/user/0/...` vs the canonical `/data/data/...`). The terminal binds
     * [Paths.home] to `/root`, so any path under it is rewritten to its home-relative
     * location under `/root`. Everything else (e.g. `/storage/...`) is already bound inside
     * the sandbox and is passed through unchanged.
     */
    private fun toContainerPath(realPath: String): String {
        val canonical = File(realPath).canonicalPath
        val home = Paths.home.absolutePath
        return if (canonical.startsWith(home)) {
            "/root" + canonical.removePrefix(home)
        } else {
            canonical
        }
    }
}

/**
 * Wraps a path in single quotes for safe use in a shell command.
 */
internal fun shellQuote(path: String): String =
    "'" + path.replace("'", "'\\''") + "'"
