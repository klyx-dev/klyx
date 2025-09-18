package com.klyx.core.event.io

import com.klyx.core.file.KxFile

data class FileOpenEvent(val file: KxFile, val worktree: KxFile? = file.parentFile)
