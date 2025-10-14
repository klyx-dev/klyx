package com.klyx.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDirection
import com.klyx.core.LocalFixedColorRoles
import com.klyx.core.theme.Contrast
import com.klyx.core.theme.FixedColorRoles
import com.klyx.core.theme.LocalContrast
import com.klyx.core.theme.LocalIsDarkMode
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.dynamicColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KlyxTheme(
    darkTheme: Boolean = LocalIsDarkMode.current,
    isHighContrastModeEnabled: Boolean = LocalContrast.current == Contrast.High,
    content: @Composable () -> Unit
) {
    val colorScheme =
        dynamicColorScheme(!darkTheme).run {
            if (isHighContrastModeEnabled && darkTheme)
                copy(
                    surface = Color.Black,
                    background = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = surfaceContainerLowest,
                    surfaceContainer = surfaceContainerLow,
                    surfaceContainerHigh = surfaceContainerLow,
                    surfaceContainerHighest = surfaceContainer,
                )
            else this
        }

    val textStyle = LocalTextStyle.current.copy(
        lineBreak = LineBreak.Paragraph,
        textDirection = TextDirection.Content,
    )

    val tonalPalettes = LocalTonalPalettes.current

    CompositionLocalProvider(
        LocalFixedColorRoles provides FixedColorRoles.fromTonalPalettes(tonalPalettes),
        LocalTextStyle provides textStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}
