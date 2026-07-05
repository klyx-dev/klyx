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

/**
 * Marks a property or function that is discouraged for use within suspending functions.
 *
 * In suspending contexts, there is often a more appropriate way to access the required
 * information (e.g., using `currentPluginContext()`). These marked declarations are
 * typically intended for use in non-suspending functions where the suspending alternatives
 * are unavailable.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This declaration is discouraged in suspending functions. Use the corresponding current*() function instead if available."
)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class DiscouragedInSuspend
