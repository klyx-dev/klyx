package com.klyx.extension.modules

import android.content.Context
import android.widget.Toast
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@HostModule("klyx:extension/system")
object System : KoinComponent, CoroutineScope by MainScope() {
    private val context: Context by inject()

    @HostFunction
    fun showToast(message: String, duration: Int) {
        launch {
            Toast.makeText(context, message, duration).show()
        }
    }
}
