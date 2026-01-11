package com.klyx.extension

class ExtensionStillInstalledException(
    val extensionId: String,
    override val message: String = "extension $extensionId is still installed",
    override val cause: Throwable? = null
) : Exception()
