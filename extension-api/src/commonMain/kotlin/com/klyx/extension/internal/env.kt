package com.klyx.extension.internal

expect fun getenv(name: String): String?
expect fun getenv(): Map<String, String>
