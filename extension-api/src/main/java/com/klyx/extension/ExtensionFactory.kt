package com.klyx.extension

import android.content.Context
import com.klyx.extension.impl.AndroidImpl
import com.klyx.extension.impl.FileSystemImpl
import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider
import kwasm.api.HostFunction
import kwasm.runtime.EmptyValue
import kwasm.runtime.IntValue

class ExtensionFactory(
    private val android: Android,
    private val fileSystem: FileSystem
) {
    fun loadExtension(extension: Extension, callStartFunction: Boolean = false): KWasmProgram {
        val (input, toml) = extension
        val id = toml.id

        val program = KWasmProgram.builder(ByteBufferMemoryProvider(toml.requestedMemorySize * 1024L * 1024L))
            .withBinaryModule(id, input)
            .withHostFunction(
                namespace = android.namespace,
                name = "show_toast_impl",
                hostFunction = HostFunction { ptr: IntValue, length: IntValue, context ->
                    val bytes = ByteArray(length.value)
                    context.memory?.readBytes(bytes, ptr.value)
                    android.showToast(bytes.toString(Charsets.UTF_8))
                    EmptyValue
                }
            ).build()

        if (callStartFunction) {
            runCatching {
                program.getFunction(id, "start")()
            }.onFailure {
                throw ExtensionLoadException("Failed to call start function: ${it.message}", it)
            }
        }

        return program
    }

    companion object {
        @JvmStatic
        fun create(context: Context) = ExtensionFactory(
            android = AndroidImpl(context),
            fileSystem = FileSystemImpl()
        )
    }
}
