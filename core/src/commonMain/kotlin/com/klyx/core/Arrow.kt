package com.klyx.core

import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <reified A> Option<A>.getOrThrow(): A {
    contract { returnsNotNull() implies (this@getOrThrow is Some<A>) }
    return getOrElse { throw NoSuchElementException("No ${A::class.simpleName} present") }
}

