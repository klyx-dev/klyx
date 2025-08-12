package com.klyx.core

actual fun Any?.identityHashCode(): Int = System.identityHashCode(this)

