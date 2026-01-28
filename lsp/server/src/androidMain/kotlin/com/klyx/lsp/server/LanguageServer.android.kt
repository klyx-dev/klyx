package com.klyx.lsp.server

import kotlinx.io.asSink
import kotlinx.io.asSource
import java.io.InputStream
import java.io.OutputStream

fun LanguageServer(
    client: LanguageClient,
    `in`: InputStream,
    out: OutputStream
): LanguageServer = LanguageServer(client, `in`.asSource(), out.asSink())
