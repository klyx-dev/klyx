package com.klyx.extension.native

fun CapabilityGrantException(message: String): CapabilityGrantException = CapabilityGrantException.Inner(message)
fun NodeRuntimeException(message: String): NodeRuntimeException = NodeRuntimeException.Internal(message)
fun HttpResponseException(message: String): HttpResponseException = HttpResponseException.Inner(message)
fun ExtensionRuntimeException(message: String): ExtensionRuntimeException = ExtensionRuntimeException.Inner(message)
fun ReadTextFileException(message: String, cause: Throwable? = null): ReadTextFileException =
    ReadTextFileException.Internal(message).apply { initCause(cause) }
