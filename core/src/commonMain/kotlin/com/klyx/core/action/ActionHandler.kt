package com.klyx.core.action

fun interface ActionHandler<A : Action> {
    suspend fun ActionContext.handle(action: A)
}
