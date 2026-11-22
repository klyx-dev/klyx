package com.klyx.core.action

import com.klyx.core.Disposable
import com.klyx.core.merge
import org.koin.core.annotation.Single

interface ActionRegistrar {
    fun registerActions(): Disposable?
}

@Single
class ActionRegistrationManager(
    private val registrars: List<ActionRegistrar>
) {
    fun register(): Disposable = registrars.map { it.registerActions() }.merge()
}
