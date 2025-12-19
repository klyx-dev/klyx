package com.klyx.extension.capabilities

import kotlinx.serialization.Serializable

@Serializable
data class NpmInstallPackageCapability(val `package`: String) {
    /**
     * Returns whether the capability allows installing the given NPM package.
     */
    fun allows(`package`: String) = this.`package` == "*" || this.`package` == `package`
}
