package com.klyx.editor.compose.selection.contextmenu

//noinspection SuspiciousImport
import android.R
import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun computeContextMenuColors(): ContextMenuColors =
    computeContextMenuColors(
        backgroundStyleId = R.style.Widget_PopupMenu,
        foregroundStyleId = R.style.TextAppearance_Widget_PopupMenu_Large,
    )

@Composable
internal fun computeContextMenuColors(
    @StyleRes backgroundStyleId: Int,
    @StyleRes foregroundStyleId: Int,
): ContextMenuColors {
    val context = LocalContext.current
    return remember(context, LocalConfiguration.current) {
        val backgroundColor =
            context.resolveColor(
                backgroundStyleId,
                R.attr.colorBackground,
                DefaultContextMenuColors.backgroundColor,
            )

        val textColorStateList =
            context.resolveColorStateList(foregroundStyleId, R.attr.textColorPrimary)
        val enabledColor = textColorStateList.enabledColor(DefaultContextMenuColors.textColor)
        val disabledColor =
            textColorStateList.disabledColor(DefaultContextMenuColors.disabledTextColor)

        ContextMenuColors(
            backgroundColor = backgroundColor,
            textColor = enabledColor,
            iconColor = enabledColor,
            disabledTextColor = disabledColor,
            disabledIconColor = disabledColor,
        )
    }
}

private fun Context.resolveColor(
    @StyleRes resId: Int,
    @AttrRes attrId: Int,
    defaultColor: Color,
): Color {
    val typedArray = obtainStyledAttributes(resId, intArrayOf(attrId))
    val defaultColorAndroid = defaultColor.toArgb()
    val colorInt = typedArray.getColor(0, defaultColorAndroid)
    typedArray.recycle()
    return if (colorInt == defaultColorAndroid) defaultColor else Color(colorInt)
}

private fun Context.resolveColorStateList(
    @StyleRes resId: Int,
    @AttrRes attrId: Int,
): ColorStateList? {
    val typedArray = obtainStyledAttributes(resId, intArrayOf(attrId))
    val colorStateList = typedArray.getColorStateList(0)
    typedArray.recycle()
    return colorStateList
}

private fun ColorStateList?.enabledColor(defaultColor: Color): Color {
    val defaultColorArgb = defaultColor.toArgb()
    val color = this?.getColorForState(intArrayOf(R.attr.state_enabled), defaultColorArgb)
    return if (color == null || color == defaultColorArgb) defaultColor else Color(color)
}

private fun ColorStateList?.disabledColor(defaultColor: Color): Color {
    val defaultColorArgb = defaultColor.toArgb()
    val color = this?.getColorForState(intArrayOf(-R.attr.state_enabled), defaultColorArgb)
    return if (color == null || color == defaultColorArgb) defaultColor else Color(color)
}
