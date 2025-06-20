package com.klyx.core.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    vararg extraModule: Module,
    config: KoinAppDeclaration? = null,
) {
    startKoin {
        config?.invoke(this)
        modules(platformModule, sharedModule, *extraModule)
    }
}
