package com.klyx.core

import androidx.compose.runtime.ProvidableCompositionLocal

/**
 * Represents a platform-specific context that acts as an interface to
 * global information about an application environment.
 */
expect abstract class PlatformContext

expect val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext>

