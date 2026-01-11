package com.klyx.editor.language.manifest

import kotlin.jvm.JvmInline

@JvmInline
value class ManifestName(val value: String) : CharSequence by value
