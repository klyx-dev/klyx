package com.klyx.core.di

import com.klyx.core.Notifier
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual val platformModule = module {
    singleOf(::Notifier)
}
