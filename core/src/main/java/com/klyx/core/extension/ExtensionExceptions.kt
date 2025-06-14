package com.klyx.core.extension

class ExtensionFetchException(
    message: String,
    throwable: Throwable? = null
) : Exception(message, throwable)

class ExtensionInstallException(
    message: String,
    throwable: Throwable? = null
) : Exception(message, throwable)
