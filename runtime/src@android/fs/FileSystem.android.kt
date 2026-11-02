package com.klyx.runtime.fs

import android.os.Environment
import io.github.vinceglb.filekit.utils.toKotlinxPath

actual val systemHomeDirectory = Environment.getExternalStorageDirectory().toKotlinxPath()
