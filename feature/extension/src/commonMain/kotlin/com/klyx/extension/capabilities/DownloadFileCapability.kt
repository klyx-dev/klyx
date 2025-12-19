package com.klyx.extension.capabilities

import io.ktor.http.Url
import kotlinx.serialization.Serializable

@Serializable
data class DownloadFileCapability(val host: String, val path: List<String>) {

    /**
     * Returns whether the capability allows downloading a file from the given URL.
     */
    fun allows(url: Url): Boolean {
        if (host != url.host && host != "*") return false

        val desiredPath = url.segments

        for ((ix, pathSegment) in path.withIndex()) {
            if (pathSegment == "**") return true
            if (ix >= desiredPath.size) return false
            if (pathSegment != "*" && pathSegment != desiredPath[ix]) return false
        }

        return path.size >= desiredPath.size
    }
}
