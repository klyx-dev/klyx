package com.klyx.core

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual fun decodeBase64(base64: String) = Base64.decode(base64)
