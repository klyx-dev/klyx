package com.klyx.api

/**
 * Marks declarations that are internal to the Klyx ecosystem.
 *
 * These APIs are not intended for public use and may change or be removed at any time.
 * Using internal APIs can lead to binary incompatibility or runtime errors in future versions.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Klyx API that should not be used from outside of klyx modules. " +
            "It may be changed or removed in the future without notice."
)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class InternalKlyxApi
