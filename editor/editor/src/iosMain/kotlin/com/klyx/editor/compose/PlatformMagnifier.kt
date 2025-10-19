@file:OptIn(ExperimentalForeignApi::class)

package com.klyx.editor.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.uikit.utils.CMPTextLoupeSession
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.updateAndGet
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIColor
import platform.UIKit.UIView
import kotlin.math.roundToInt

@Stable
internal interface PlatformMagnifierFactory {

    fun create(
        view: UIView,
        density: Density,
        color: Color
    ): PlatformMagnifier

    companion object {
        @Stable
        fun getForCurrentPlatform(): PlatformMagnifierFactory =
            when {
                !isPlatformMagnifierSupported() -> {
                    throw UnsupportedOperationException(
                        "Magnifier is only supported on iOS 17 and higher."
                    )
                }

                else -> PlatformMagnifierFactoryIos17Impl
            }
    }
}

internal interface PlatformMagnifier {

    /** Returns the actual size of the magnifier widget, even if not specified at creation. */
    val size: IntSize

    /** Causes the magnifier to re-copy the magnified pixels.*/
    fun updateContent()

    /**
     * Sets the properties on a Magnifier instance that can be updated without recreating the
     * magnifier.
     */
    fun update(sourceCenter: Offset)

    fun dismiss()
}

@Stable
internal object PlatformMagnifierFactoryIos17Impl : PlatformMagnifierFactory {

    override fun create(
        view: UIView,
        density: Density,
        color: Color
    ): PlatformMagnifier {

        val tint = color.takeIf { it.isSpecified }?.let {
            UIColor(
                red = it.red.toDouble(),
                green = it.green.toDouble(),
                blue = it.blue.toDouble(),
                alpha = it.alpha.toDouble(),
            )
        }

        return PlatformMagnifierImpl(
            density = density.density,
            loupeSessionFactory = {
                val lastTint = view.tintColor

                if (tint != null) {
                    // magnifier border color depends on view tint
                    view.tintColor = tint
                }

                checkNotNull(
                    CMPTextLoupeSession.beginLoupeSessionAtPoint(
                        point = CGPointMake(it.x.toDouble(), it.y.toDouble()),
                        fromSelectionWidgetView = null,
                        inView = view
                    )
                ).also {
                    // restore tint to previous value
                    // magnifer won't change its color until the next beginLoupeSessionAtPoint call
                    view.tintColor = lastTint
                }
            }
        )
    }

    class PlatformMagnifierImpl(
        val density: Float,
        val loupeSessionFactory: (Offset) -> CMPTextLoupeSession
    ) : PlatformMagnifier {

        override val size: IntSize = IntSize(
            (115 * density).roundToInt(),
            (80 * density).roundToInt()
        )

        private val loupeSession = atomic<CMPTextLoupeSession?>(null)

        override fun updateContent() {
            // is not required. loupe redraws automatically
        }

        override fun update(sourceCenter: Offset) {

            if (sourceCenter.isUnspecified)
                return

            val sourceCenterDp = sourceCenter / density

            val session = loupeSession.value
                ?: loupeSession.updateAndGet { it ?: loupeSessionFactory(sourceCenterDp) }

            val sourceCenterPoint = CGPointMake(
                sourceCenterDp.x.toDouble(),
                sourceCenterDp.y.toDouble()
            )

            session?.moveToPoint(
                point = sourceCenterPoint,
                withCaretRect = CGRectZero.readValue(),
                trackingCaret = false
            )
        }

        override fun dismiss() {
            loupeSession.getAndUpdate { null }?.invalidate()
        }
    }
}
