package com.klyx.editor.compose.internal

internal expect class SynchronizedObject

/**
 * Returns [ref] as a [SynchronizedObject] on platforms where [Any] is a valid [SynchronizedObject],
 * or a new [SynchronizedObject] instance if [ref] is null or this is not supported on the current
 * platform.
 */
internal expect inline fun makeSynchronizedObject(ref: Any? = null): SynchronizedObject

internal expect inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R
