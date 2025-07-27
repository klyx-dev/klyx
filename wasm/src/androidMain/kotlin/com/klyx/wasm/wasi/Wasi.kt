package com.klyx.wasm.wasi

import android.util.Log
import com.dylibso.chicory.log.Logger

@DslMarker
internal annotation class WasiDsl

object WasiLogger : Logger {
    private const val TAG = "[KlyxWasi]"

    override fun log(
        level: Logger.Level,
        msg: String?,
        throwable: Throwable?
    ) {
        when (level) {
            Logger.Level.ALL -> Log.i(TAG, msg, throwable)
            Logger.Level.TRACE -> Log.v(TAG, msg, throwable)
            Logger.Level.DEBUG -> Log.d(TAG, msg, throwable)
            Logger.Level.INFO -> Log.i(TAG, msg, throwable)
            Logger.Level.WARNING -> Log.w(TAG, msg, throwable)
            Logger.Level.ERROR -> Log.e(TAG, msg, throwable)
            Logger.Level.OFF -> {}
        }
    }

    override fun isLoggable(level: Logger.Level): Boolean {
        return Log.isLoggable(
            TAG, when (level) {
                Logger.Level.ALL -> Log.VERBOSE
                Logger.Level.TRACE -> Log.VERBOSE
                Logger.Level.DEBUG -> Log.DEBUG
                Logger.Level.INFO -> Log.INFO
                Logger.Level.WARNING -> Log.WARN
                Logger.Level.ERROR -> Log.ERROR
                Logger.Level.OFF -> Int.MAX_VALUE // effectively disable logging for OFF level
            }
        )
    }
}
