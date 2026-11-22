package com.klyx.lsp.process

import io.matthewnelson.kmp.process.Process
import kotlinx.io.asSink
import kotlinx.io.asSource

actual fun Process.asRawStreams(): ProcessStreams {
    val jProcessField = runCatching {
        var clazz: Class<*>? = this::class.java
        var field: java.lang.reflect.Field? = null
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField("jProcess")
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        field?.apply { isAccessible = true }
    }.getOrNull()

    if (jProcessField != null) {
        val jProcess = jProcessField.get(this) as java.lang.Process
        return ProcessStreams(jProcess.inputStream.asSource(), jProcess.outputStream.asSink())
    }
    error("Could not access underlying process streams for ${this::class.qualifiedName}")
}
