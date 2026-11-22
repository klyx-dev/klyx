@file:OptIn(ExperimentalNativeApi::class)

package com.klyx.core

import kotlin.experimental.ExperimentalNativeApi

actual fun App.isDebugMode() = Platform.isDebugBinary
