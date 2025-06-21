package com.klyx

class Greeting {
    private val platform = platform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}
