package com.klyx.extension.modules

import android.content.Context
import android.widget.Toast
import com.klyx.wasm.ExperimentalWasm
import com.klyx.wasm.HostModule
import com.klyx.wasm.HostModuleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(ExperimentalWasm::class)
class SystemModule : HostModule, KoinComponent {
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
