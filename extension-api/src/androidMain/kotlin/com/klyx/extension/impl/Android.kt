package com.klyx.extension.impl

import android.content.Context
import android.widget.Toast
import com.klyx.extension.ExtensionHostModule
import com.klyx.extension.HostFunctionDefinition
import com.klyx.extension.wasm.string
import kwasm.api.HostFunctionContext
import kwasm.api.UnitHostFunction
import kwasm.runtime.IntValue

class Android(private val context: Context) : ExtensionHostModule {
    private fun showToast(ptr: IntValue, len: IntValue, context: HostFunctionContext) {
        Toast.makeText(this.context, string(ptr, len, context), Toast.LENGTH_SHORT).show()
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
