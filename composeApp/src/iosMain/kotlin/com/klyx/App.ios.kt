package com.klyx

actual fun requestFileAccessPermission() {
    // No-op: iOS handles permissions via sandboxing / user prompts at file picker
}
