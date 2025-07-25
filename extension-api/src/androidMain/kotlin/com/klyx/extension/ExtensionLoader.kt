package com.klyx.extension

import android.content.Context
import android.widget.Toast
import com.klyx.core.extension.Extension
import com.klyx.core.theme.ThemeManager
import com.klyx.expect
import com.klyx.wasm.WasmType
import com.klyx.wasm.readString
import com.klyx.wasm.wasm
import kotlinx.io.asSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ExtensionLoader : KoinComponent {
    private val context: Context by inject()

    suspend fun loadExtension(
        extension: Extension,
        shouldCallInit: Boolean = false
    ) {
        extension.themeFiles.forEach { file ->
            if (file.inputStream() == null) return@forEach

            ThemeManager
                .loadThemeFamily(file.inputStream()!!.asSource())
                .expect("Unable to load the theme")
        }

        extension.wasmFiles.firstOrNull()?.let { wasm ->
            wasm {
                module { bytes(wasm.readBytes()) }
                callInit(enabled = shouldCallInit)

                function(
                    namespace = "Android",
                    name = "show_toast_impl",
                    params = listOf(WasmType.I32, WasmType.I32)
                ) { instance, args ->
                    val message = instance.memory.readString(args)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
