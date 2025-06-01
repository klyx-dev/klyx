package com.klyx.extension

class ExtensionLoadException(
    override val message: String? = "Extension load failed",
    override val cause: Throwable? = null
) : Exception()
