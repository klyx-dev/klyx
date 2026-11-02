package com.klyx.runtime.fs

import kotlinx.io.files.Path

interface FileSystem {
    companion object
}

expect val systemHomeDirectory: Path
