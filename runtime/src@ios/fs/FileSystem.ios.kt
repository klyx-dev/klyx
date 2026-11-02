package com.klyx.runtime.fs

import kotlinx.io.files.Path
import platform.Foundation.NSHomeDirectory

actual val systemHomeDirectory = Path(NSHomeDirectory())
