package com.klyx.nodegraph

import kotlin.uuid.Uuid

interface GraphExecutionListener {
    /** Called when execution reaches a node. */
    fun onNodeEnter(nodeId: Uuid) {}

    /** Called when a node produces a log line via the log callback. */
    fun onLog(message: String) {}

    /** Called when execution completes successfully. */
    fun onComplete() {}

    /** Called when execution cannot start (no entry node found etc.). */
    fun onError(message: String) {}
}
