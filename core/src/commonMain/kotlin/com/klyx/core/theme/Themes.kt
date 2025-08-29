package com.klyx.core.theme

import com.klyx.core.theme.autumnEmber.darkScheme
import com.klyx.core.theme.autumnEmber.highContrastDarkColorScheme
import com.klyx.core.theme.autumnEmber.highContrastLightColorScheme
import com.klyx.core.theme.autumnEmber.lightScheme
import com.klyx.core.theme.autumnEmber.mediumContrastDarkColorScheme
import com.klyx.core.theme.autumnEmber.mediumContrastLightColorScheme
import com.klyx.core.theme.emeraldWaves.darkScheme
import com.klyx.core.theme.emeraldWaves.highContrastDarkColorScheme
import com.klyx.core.theme.emeraldWaves.highContrastLightColorScheme
import com.klyx.core.theme.emeraldWaves.lightScheme
import com.klyx.core.theme.emeraldWaves.mediumContrastDarkColorScheme
import com.klyx.core.theme.emeraldWaves.mediumContrastLightColorScheme
import com.klyx.core.theme.goldenGlow.darkScheme
import com.klyx.core.theme.goldenGlow.highContrastDarkColorScheme
import com.klyx.core.theme.goldenGlow.highContrastLightColorScheme
import com.klyx.core.theme.goldenGlow.lightScheme
import com.klyx.core.theme.goldenGlow.mediumContrastDarkColorScheme
import com.klyx.core.theme.goldenGlow.mediumContrastLightColorScheme
import com.klyx.core.theme.oceanBreeze.darkScheme
import com.klyx.core.theme.oceanBreeze.highContrastDarkColorScheme
import com.klyx.core.theme.oceanBreeze.highContrastLightColorScheme
import com.klyx.core.theme.oceanBreeze.lightScheme
import com.klyx.core.theme.oceanBreeze.mediumContrastDarkColorScheme
import com.klyx.core.theme.oceanBreeze.mediumContrastLightColorScheme

object AutumnEmber
object EmeraldWaves
object OceanBreeze
object GoldenGlow

fun AutumnEmber.asTheme(): Theme {
    return Theme(
        name = "Autumn Ember",
        lightScheme = lightScheme,
        darkScheme = darkScheme,
        lightSchemeMediumContrast = mediumContrastLightColorScheme,
        lightSchemeHighContrast = highContrastLightColorScheme,
        darkSchemeMediumContrast = mediumContrastDarkColorScheme,
        darkSchemeHighContrast = highContrastDarkColorScheme
    )
}

fun EmeraldWaves.asTheme(): Theme {
    return Theme(
        name = "Emerald Waves",
        lightScheme = lightScheme,
        darkScheme = darkScheme,
        lightSchemeMediumContrast = mediumContrastLightColorScheme,
        lightSchemeHighContrast = highContrastLightColorScheme,
        darkSchemeMediumContrast = mediumContrastDarkColorScheme,
        darkSchemeHighContrast = highContrastDarkColorScheme
    )
}

fun OceanBreeze.asTheme(): Theme {
    return Theme(
        name = "Ocean Breeze",
        lightScheme = lightScheme,
        darkScheme = darkScheme,
        lightSchemeMediumContrast = mediumContrastLightColorScheme,
        lightSchemeHighContrast = highContrastLightColorScheme,
        darkSchemeMediumContrast = mediumContrastDarkColorScheme,
        darkSchemeHighContrast = highContrastDarkColorScheme
    )
}

fun GoldenGlow.asTheme(): Theme {
    return Theme(
        name = "Golden Glow",
        lightScheme = lightScheme,
        darkScheme = darkScheme,
        lightSchemeMediumContrast = mediumContrastLightColorScheme,
        lightSchemeHighContrast = highContrastLightColorScheme,
        darkSchemeMediumContrast = mediumContrastDarkColorScheme,
        darkSchemeHighContrast = highContrastDarkColorScheme
    )
}
