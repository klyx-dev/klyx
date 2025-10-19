package com.klyx.editor.compose.input

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

internal class EditorInputHandler(looper: Looper) : Handler(looper)

internal fun EditorInputHandler(name: String = "EditorInputHandler"): EditorInputHandler {
    val thread = HandlerThread(name).also { it.start() }
    return EditorInputHandler(thread.looper)
}

