package com.klyx.core.process

import io.matthewnelson.kmp.file.absolutePath2
import java.io.File

@OptIn(InternalProcessApi::class)
fun KxProcess.asJavaProcessBuilder(): ProcessBuilder {
    val raw = if (isSpawned) raw else spawn().raw
    return raw.let { p ->
        ProcessBuilder(p.command, *p.args.toTypedArray()).apply {
            environment().putAll(p.environment)
            p.cwd?.let { dir -> directory(File(dir.absolutePath2())) }
        }.also {
            p.destroy()
        }
    }
}
