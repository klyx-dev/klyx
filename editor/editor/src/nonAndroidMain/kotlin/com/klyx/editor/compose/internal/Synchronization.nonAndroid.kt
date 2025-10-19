@file:JvmName("SynchronizationKt")

package com.klyx.editor.compose.internal

import kotlin.jvm.JvmName

internal actual class SynchronizedObject : kotlinx.atomicfu.locks.SynchronizedObject()

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun makeSynchronizedObject(ref: Any?) = SynchronizedObject()

internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R {
    return kotlinx.atomicfu.locks.synchronized(lock, block)
}
