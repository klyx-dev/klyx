package com.klyx.runner

import com.klyx.core.file.KxFile
import com.klyx.core.runner.CodeRunner

actual fun KxFile.runner(): CodeRunner {
    return UniversalRunner()
}

actual fun CodeRunner(): CodeRunner = UniversalRunner()
