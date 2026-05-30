package com.klyx.data.fs

data class FileCapabilities(
    val canWrite: Boolean,
    val canDelete: Boolean,
    val canRename: Boolean,
    val canCreate: Boolean,

)
