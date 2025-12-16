package com.klyx.core.app

import com.klyx.core.process.Thread

expect fun setupCrashHandler(onCrash: (Thread, Throwable) -> Unit = { _, throwable -> throwable.printStackTrace() })
