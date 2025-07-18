package com.klyx.extension.impl

import android.content.Context
import android.widget.Toast
import com.klyx.extension.ExtensionHostModule
import com.klyx.extension.HostFunctionDefinition
import com.klyx.extension.wasm.string
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kwasm.api.HostFunctionContext
import kwasm.api.UnitHostFunction
import kwasm.runtime.IntValue

class Android(private val context: Context) : ExtensionHostModule {
    private val scope = MainScope()

    private fun showToast(
        ptr: IntValue,
        len: IntValue,
        duration: IntValue,
        context: HostFunctionContext
    ) {
        val duration = when (duration.value) {
            0 -> Toast.LENGTH_SHORT
            1 -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }

        scope.launch {
            Toast.makeText(this@Android.context, string(ptr, len, context), duration).show()
        }
    }

    override val namespace: String
        get() = "Android"

    override fun getHostFunctions() = listOf(
        HostFunctionDefinition(
            "show_toast",
            UnitHostFunction(::showToast)
        )
    )
}
