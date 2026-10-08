package com.klyx.core

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
