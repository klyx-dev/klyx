package com.klyx.lsp.server

class ServerAlreadyInitializedException(
    override val message: String? = "Server is already initialized",
    override val cause: Throwable? = null
) : Exception()
