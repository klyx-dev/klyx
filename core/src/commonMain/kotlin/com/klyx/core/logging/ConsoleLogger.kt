package com.klyx.core.logging

expect object ConsoleLogger : Logger {
    override fun log(message: Message)
}
