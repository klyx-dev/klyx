package com.klyx.core.file

import com.klyx.core.PlatformContext
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController

actual fun PlatformContext.shareFile(file: KxFile) {
    val url = NSURL.fileURLWithPath(file.absolutePath)
    val activityVC = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null
    )
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVC?.presentViewController(activityVC, animated = true, completion = null)
}

actual fun openFile(file: KxFile) {
    val url = NSURL.fileURLWithPath(file.absolutePath)
    val controller = UIDocumentInteractionController.interactionControllerWithURL(url)
    controller.presentPreviewAnimated(true)
}
