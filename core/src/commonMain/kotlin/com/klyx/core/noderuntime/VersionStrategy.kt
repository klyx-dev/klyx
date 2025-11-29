package com.klyx.core.noderuntime

sealed interface VersionStrategy {
    /**
     * Install if current version doesn't match pinned version
     */
    data class Pin(val pinnedVersion: String) : VersionStrategy

    /**
     * Install if current version is older than latest version
     */
    data class Latest(val latestVersion: String) : VersionStrategy
}
