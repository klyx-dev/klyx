package com.klyx

import com.klyx.core.CoreModule
import com.klyx.runtime.RuntimeModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [CoreModule::class, RuntimeModule::class])
@ComponentScan("com.klyx")
object AppModule
