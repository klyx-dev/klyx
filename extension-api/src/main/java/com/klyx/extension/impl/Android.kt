package com.klyx.extension.impl

import android.content.Context
import com.klyx.core.showShortToast
import com.klyx.extension.ExtensionHostModule
import com.klyx.extension.HostFunctionDefinition
import com.klyx.extension.wasm.string
import kwasm.api.HostFunctionContext
import kwasm.api.UnitHostFunction
import kwasm.runtime.IntValue

class Android(private val context: Context) : ExtensionHostModule {
    private fun showToast(ptr: Int, len: Int, context: HostFunctionContext) {
        this.context.showShortToast(string(ptr, len, context))
    }

    override val namespace: String
        get() = "Android"

    override fun getHostFunctions() = listOf(
        HostFunctionDefinition("show_toast", UnitHostFunction { ptr: IntValue, len: IntValue, ctx -> showToast(ptr.value, len.value, ctx) })
    )
}
