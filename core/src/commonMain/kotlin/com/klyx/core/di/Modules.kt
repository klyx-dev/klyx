package com.klyx.core.di

import com.klyx.core.Notifier
import com.klyx.core.notification.NotificationManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    singleOf(::NotificationManager)
    singleOf(::Notifier)
}
