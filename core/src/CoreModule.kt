package com.klyx.core

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** Koin module for core's own annotated definitions. */
@Module
@ComponentScan("com.klyx.core")
object CoreModule
