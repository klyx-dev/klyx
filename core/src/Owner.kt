package com.klyx.core

import com.klyx.core.action.Registration

interface Owner {
    val ownerId: String

    /** False once the owner has been unloaded. */
    val isActive: Boolean get() = true
}

/** Something that can be released. */
fun interface Disposable {
    fun dispose()
}

fun List<Disposable>.merge() = Disposable { asReversed().forEach { it.dispose() } }

fun List<Registration>.asDisposable() = Disposable { forEach { it.unregister() } }
