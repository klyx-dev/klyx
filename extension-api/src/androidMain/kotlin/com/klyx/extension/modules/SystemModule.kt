package com.klyx.extension.modules

import android.content.Context
import android.widget.Toast
import com.klyx.wasm.HostModule
import com.klyx.wasm.HostModuleScope
import com.klyx.wasm.i32
import com.klyx.wasm.readString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SystemModule : HostModule, KoinComponent {
    private val context: Context by inject()
    override val name = "klyx:extension/system"

    override fun HostModuleScope.functions() {
        function("show-toast", params = string + i32) { instance, args ->
            val message = instance.memory.readString(args)
            val duration = args[2].i32

            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, duration).show()
            }
        }
    }
}
