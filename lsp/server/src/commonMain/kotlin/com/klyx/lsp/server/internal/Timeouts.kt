package com.klyx.lsp.server.internal

import kotlin.time.Duration.Companion.seconds

val LSP_REQUEST_TIMEOUT = (60 * 2).seconds
internal val SERVER_SHUTDOWN_TIMEOUT = 5.seconds
