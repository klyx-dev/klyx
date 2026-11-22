package com.klyx.runtime.workspace

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ProjectId(val value: String)

@JvmInline
@Serializable
value class DocumentId(val value: String)

@JvmInline
@Serializable
value class EditorGroupId(val value: String)
