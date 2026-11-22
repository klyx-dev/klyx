package com.klyx.runtime.fs

import kotlinx.io.files.Path

actual val systemHomeDirectory = Path(System.getProperty("user.home"))
