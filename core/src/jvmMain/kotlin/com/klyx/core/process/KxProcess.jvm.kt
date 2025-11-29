package com.klyx.core.process

import io.matthewnelson.kmp.process.changeDir
import kotlinx.io.files.Path
import java.io.File

actual val SystemPathSeparator: Char = File.pathSeparatorChar

actual fun KxProcessBuilder.changeDir(dir: Path) {
    changeDir(File(dir.toString()))
}
