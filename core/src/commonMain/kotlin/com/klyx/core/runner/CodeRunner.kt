package com.klyx.core.runner

import com.klyx.core.file.KxFile

interface CodeRunner {

    fun canRun(file: KxFile): Boolean

    suspend fun run(file: KxFile)
}
