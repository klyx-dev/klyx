package com.klyx.core.process

import com.klyx.core.unsupported
import kotlinx.io.files.Path

actual val SystemPathSeparator: Char = ':'

actual fun KxProcessBuilder.changeDir(dir: Path) {
    unsupported("iOS does not support changing directories")
}
