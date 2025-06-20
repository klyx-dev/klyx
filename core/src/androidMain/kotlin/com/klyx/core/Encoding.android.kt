package com.klyx.core

import android.util.Base64

actual fun decodeBase64(base64: String): ByteArray {
    return Base64.decode(base64, Base64.DEFAULT)
}
