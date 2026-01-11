package com.klyx.extension.host

import com.klyx.core.app.App
import com.klyx.core.app.Global
import com.klyx.core.file.toOkioPath
import com.klyx.core.io.Paths
import com.klyx.core.io.extensionsDir
import com.klyx.core.noderuntime.NodeRuntime
import com.klyx.extension.ExtensionHostProxy

@JvmInline
value class GlobalExtensionStore(val store: ExtensionStore) : Global

fun initExtensionHost(extensionHostProxy: ExtensionHostProxy, nodeRuntime: NodeRuntime, cx: App) {
    val store = ExtensionStore(
        extensionsDirectory = Paths.extensionsDir.toOkioPath(),
        proxy = extensionHostProxy,
        nodeRuntime = nodeRuntime,
        buildDirectory = null,
        cx = cx
    )
    cx.setGlobal(GlobalExtensionStore(store))
}
