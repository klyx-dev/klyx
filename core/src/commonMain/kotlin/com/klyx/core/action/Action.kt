package com.klyx.core.action

import kotlin.reflect.KClass

interface Action

fun interface Registration {
    fun unregister()
}

class ActionException(val action: Action, cause: Throwable) :
    RuntimeException("Action $action failed: ${cause.message}", cause)

class ActionNotRegisteredException(val type: KClass<*>) :
    IllegalStateException("No handler registered for ${type.simpleName}")
