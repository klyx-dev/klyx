package com.klyx

actual fun requestFileAccessPermission() {
    // No-op for JVM, permissions are controlled by OS
}
