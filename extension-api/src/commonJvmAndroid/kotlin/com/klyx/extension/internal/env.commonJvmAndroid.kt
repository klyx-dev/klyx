package com.klyx.extension.internal

actual fun getenv(name: String): String? = System.getenv(name)

actual fun getenv(): Map<String, String> = System.getenv()
