package com.klyx.core.io

import com.klyx.core.file.toOkioPath
import com.klyx.core.logging.logger
import com.klyx.core.platform.Os.Companion.Android
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.SYSTEM

/**
 * A [FileSystem] implementation designed for the `proot` sandbox environment running on [Android].
 *
 * This file system intercepts file operations and re-roots paths to a specific sandbox directory
 * defined by [Paths.root]. While it acts as a transparent wrapper around [SYSTEM],
 * it ensures that all file access occurs within the confined sandbox structure typical of
 * terminal on [Android].
 */
object SandboxFileSystem : ForwardingFileSystem(FileSystem.SYSTEM) {

    private val root = Paths.root.toOkioPath()
    private val logger = logger()

    override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
        logger.debug { "onPathParameter($path, $functionName, $parameterName)" }
        return root / path
    }

    override fun onPathResult(path: Path, functionName: String): Path {
        logger.debug { "onPathResult($path, $functionName)" }
        return path.relativeTo(root)
    }
}
