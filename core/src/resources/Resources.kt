package com.klyx.core.resources

import com.klyx.resources.Resources
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

typealias Strings = Resources.string

fun StringResource.get(): String = runBlocking { getString(this@get) }
