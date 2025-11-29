@file:OptIn(ExperimentalContracts::class)

package com.klyx.core

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <reified A> Option<A>.getOrThrow(): A {
    contract { returnsNotNull() implies (this@getOrThrow is Some<A>) }
    return getOrElse { throw NoSuchElementException("No ${A::class.simpleName} present") }
}

inline fun <A> Option<A>.expect(lazyMessage: () -> String): A {
    contract {
        callsInPlace(lazyMessage, InvocationKind.AT_MOST_ONCE)
        returns() implies (this@expect is Some<A>)
    }

    onSome { return it }

    val message = lazyMessage()
    throw IllegalStateException(message)
}

inline fun <reified A> Option<A>.expect(): A {
    contract {
        returns() implies (this@expect is Some<A>)
    }

    onSome { return it }
    throw NoSuchElementException("No ${A::class.simpleName} present in Option")
}

inline fun <T> Option<T>.unwrapOrDefault(default: () -> T): T = getOrElse(default)
fun Option<String>.unwrapOrDefault() = getOrElse { "" }

inline fun <reified T> Option<T>.unwrap() = getOrThrow()

inline fun <A> Option<A>.orElse(other: () -> Option<A>): Option<A> {
    contract { callsInPlace(other, InvocationKind.AT_MOST_ONCE) }
    return fold({ other() }, { this })
}

infix fun <A> Option<A>.or(other: Option<A>) = orElse { other }

fun <T> T?.orNone(): Option<T> = this?.let { Some(it) } ?: None

