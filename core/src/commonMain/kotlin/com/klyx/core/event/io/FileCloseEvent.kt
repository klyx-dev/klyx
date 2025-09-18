package com.klyx.core.event.io

import com.klyx.core.file.KxFile

data class FileCloseEvent(val file: KxFile, val worktree: KxFile? = file.parentFile)
