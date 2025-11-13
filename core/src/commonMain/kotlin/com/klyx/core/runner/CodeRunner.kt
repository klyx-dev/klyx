package com.klyx.core.runner

import com.klyx.core.file.KxFile

interface CodeRunner {
    suspend fun run(file: KxFile)
}
