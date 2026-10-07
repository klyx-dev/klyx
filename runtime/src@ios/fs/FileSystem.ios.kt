package com.klyx.runtime.fs

import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

actual val systemHomeDirectory = NSHomeDirectory().toPath()
