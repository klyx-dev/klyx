package com.klyx.core.di

import com.klyx.core.Notifier
import com.klyx.core.file.FileDownloader
import com.klyx.core.httpClient
import com.klyx.core.notification.NotificationManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    single { httpClient }
    singleOf(::NotificationManager)
    singleOf(::Notifier)
    singleOf(::FileDownloader)
}
