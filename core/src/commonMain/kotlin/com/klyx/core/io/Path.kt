package com.klyx.core.io

import com.klyx.fs.canExecute
import okio.Path

fun Path.isExecutable() = toString().canExecute()
