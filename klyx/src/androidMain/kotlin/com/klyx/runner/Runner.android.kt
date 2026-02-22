package com.klyx.runner

import com.klyx.core.file.KxFile
import com.klyx.core.runner.CodeRunner

private val NotCodeRunner = object : CodeRunner {
    override fun canRun(file: KxFile): Boolean {
        return false
    }

    override suspend fun run(file: KxFile) {
        TODO("Not yet implemented")
    }
}

actual fun KxFile.runner(): CodeRunner {
    return NotCodeRunner
}

actual fun CodeRunner(): CodeRunner = NotCodeRunner
