package com.klyx.runtime.fs

import okio.Path.Companion.toPath

actual val systemHomeDirectory = System.getProperty("user.home").toPath()
