package com.klyx.api.data.fs

data class SizeProgress(
    val bytes: Long,
    val fileCount: Int,
    val dirCount: Int,
    val isFinished: Boolean
)
