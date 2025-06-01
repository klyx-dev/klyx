package com.klyx.extension.impl

import android.util.Log
import com.klyx.extension.ExtensionHostModule
import com.klyx.extension.HostFunctionDefinition
import com.klyx.extension.wasm.toHostFunction

class Logger : ExtensionHostModule {
    companion object {
        private const val TAG = "Extension"
    }

    private fun log(message: String) {
        logDebug(message)
    }

    private fun logDebug(message: String) {
        Log.d(TAG, message)
    }

    private fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    private fun logWarn(message: String) {
        Log.w(TAG, message)
    }

    private fun logError(message: String) {
        Log.e(TAG, message)
    }

    override val namespace: String
        get() = "Logger"

    override fun getHostFunctions() = listOf(
        HostFunctionDefinition("log", ::log.toHostFunction()),
        HostFunctionDefinition("log_debug", ::logDebug.toHostFunction()),
        HostFunctionDefinition("log_info", ::logInfo.toHostFunction()),
        HostFunctionDefinition("log_warn", ::logWarn.toHostFunction()),
        HostFunctionDefinition("log_error", ::logError.toHostFunction()),
    )
}
