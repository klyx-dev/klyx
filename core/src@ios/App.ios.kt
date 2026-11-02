package com.klyx.core

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual fun App.isDebugMode(): Boolean = Platform.isDebugBinary
