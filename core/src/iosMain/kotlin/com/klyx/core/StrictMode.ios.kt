package com.klyx.core

import androidx.annotation.MainThread

@MainThread
actual inline fun <T> allowDiskReads(block: () -> T): T = block()

@MainThread
actual inline fun <T> allowDiskWrites(block: () -> T): T = block()

@MainThread
actual inline fun <T> allowDiskReadsWrites(block: () -> T): T = block()
