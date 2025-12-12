package com.klyx.core.io

import kotlinx.io.files.SystemFileSystem
import okio.FileSystem
import okio.SYSTEM

inline val fs get() = SystemFileSystem
inline val okioFs get() = FileSystem.SYSTEM
