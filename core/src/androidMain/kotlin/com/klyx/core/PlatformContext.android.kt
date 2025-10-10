package com.klyx.core

import androidx.compose.ui.platform.LocalContext

actual typealias PlatformContext = android.content.Context

actual inline val LocalPlatformContext get() = LocalContext
