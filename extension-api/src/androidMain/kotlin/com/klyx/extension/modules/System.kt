package com.klyx.extension.modules

import com.klyx.core.Notifier
import com.klyx.core.notification.Toast
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@HostModule("klyx:extension/system")
object System : KoinComponent, CoroutineScope by MainScope() {
    private val notifier: Notifier by inject()

    @HostFunction
    fun showToast(message: String, duration: Int) {
        val durationMillis = when (duration) {
            0 -> Toast.LENGTH_SHORT
            1 -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }
        notifier.toast(message, durationMillis)
    }
}
