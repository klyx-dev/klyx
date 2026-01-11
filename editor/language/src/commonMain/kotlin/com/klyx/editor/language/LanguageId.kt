package com.klyx.editor.language

import kotlinx.atomicfu.atomic
import kotlin.jvm.JvmInline

private val NEXT_LANGUAGE_ID = atomic(0)

@JvmInline
value class LanguageId(val value: Int)

internal fun LanguageId() = LanguageId(NEXT_LANGUAGE_ID.getAndIncrement())
