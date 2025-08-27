package com.klyx.core

import platform.Foundation.NSThread

actual val currentThreadName: String
    get() = NSThread.currentThread.name ?: "Thread-${NSThread.currentThread.hash}"
