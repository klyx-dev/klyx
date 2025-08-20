package com.klyx.core

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

actual fun string(
    resource: StringResource,
    vararg formatArgs: Any?
) = runBlocking { getString(resource, formatArgs) }
