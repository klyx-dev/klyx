package com.klyx.runtime.fs

import okio.Path

interface FileSystem {
    companion object
}

expect val systemHomeDirectory: Path
