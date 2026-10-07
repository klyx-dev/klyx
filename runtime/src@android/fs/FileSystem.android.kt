package com.klyx.runtime.fs

import android.os.Environment
import okio.Path.Companion.toOkioPath

actual val systemHomeDirectory = Environment.getExternalStorageDirectory().toOkioPath()
