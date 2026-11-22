package com.klyx.core

object Log {
    typealias Sink = (level: String, tag: String, message: String, throwable: Throwable?) -> Unit

    var sink: Sink? = null

    fun d(tag: String, message: String) = emit("D", tag, message, null)
    fun i(tag: String, message: String) = emit("I", tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = emit("W", tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = emit("E", tag, message, throwable)

    private fun emit(level: String, tag: String, message: String, throwable: Throwable?) {
        val current = sink
        if (current != null) {
            current(level, tag, message, throwable)
        } else {
            println("[$level/$tag] $message")
            throwable?.printStackTrace()
        }
    }
}
