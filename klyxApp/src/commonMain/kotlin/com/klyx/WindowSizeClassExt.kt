package com.klyx

import androidx.window.core.layout.WindowSizeClass

inline val WindowSizeClass.isWidthAtLeastMediumOrExpanded: Boolean
    get() {
        return isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ||
                isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    }
