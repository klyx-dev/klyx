package com.klyx.terminal.internal

import android.content.Context
import com.klyx.terminal.localProcess
import com.klyx.terminal.ubuntuDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

context(context: Context)
suspend fun runInProot(
    vararg args: String,
    onError: (String) -> Unit = {},
    onOutput: (String) -> Unit = {},
) = withContext(Dispatchers.IO) {
    if (!ubuntuDir.exists() || ubuntuDir.list()?.isEmpty() == true) {
        throw RuntimeException("ubuntu is not setup correctly")
    }

    localProcess("proot", "-0", "-r", ubuntuDir.absolutePath, *args) {
        onOutput(onOutput)
        onError(onError)
    }.execute()
}
