package com.klyx.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionCurveEaseOut

@OptIn(ExperimentalForeignApi::class)
actual fun Platform.showToast(
    message: String,
    duration: ToastDuration
) {
    val duration = when (duration) {
        ToastDuration.Short -> 2.0
        ToastDuration.Long -> 5.0
    }

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    val toastLabel = UILabel(
        frame = CGRectMake(
            x = 0.0,
            y = 0.0,
            width = UIScreen.mainScreen.bounds.useContents { size.width } - 40.0,
            height = 35.0
        )
    ).apply {
        center = CGPointMake(
            x = UIScreen.mainScreen.bounds.useContents { size.width } / 2,
            y = UIScreen.mainScreen.bounds.useContents { size.width } - 100.0
        )
        textAlignment = NSTextAlignmentCenter
        backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.6)
        textColor = UIColor.whiteColor
        text = message
        alpha = 1.0
        layer.cornerRadius = 15.0
        clipsToBounds = true
    }

    rootViewController?.view?.addSubview(toastLabel)

    UIView.animateWithDuration(
        duration = duration,
        delay = 0.1,
        options = UIViewAnimationOptionCurveEaseOut,
        animations = { toastLabel.alpha = 0.0 },
        completion = { if (it) toastLabel.removeFromSuperview() }
    )
}
