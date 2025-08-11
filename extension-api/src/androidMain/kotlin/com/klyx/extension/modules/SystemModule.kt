package com.klyx.extension.modules

import android.content.Context
import android.widget.Toast
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostModule
import com.klyx.wasm.HostModuleScope
import com.klyx.wasm.utils.i32
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(ExperimentalWasmApi::class)
class SystemModule : HostModule, KoinComponent, CoroutineScope by MainScope() {
    private val context: Context by inject()
    override val name = "klyx:extension/system"

    override fun HostModuleScope.functions() {
        function("show-toast", params = string + i32) { args ->
            val message = args.string
            val duration = args.i32(index = 2)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, duration).show()
            }
        }
    }
}
