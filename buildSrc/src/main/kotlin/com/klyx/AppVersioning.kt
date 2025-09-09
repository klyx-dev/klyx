package com.klyx

import io.github.z4kn4fein.semver.Version

object AppVersioning {
    private val baseVersion = Version(
        major = 1,
        minor = 7,
        patch = 3,
        //buildMetadata = "build"
    )

    const val DEBUG_SUFFIX = "dev"

    val stableVersionName = "${baseVersion.major}.${baseVersion.minor}.${baseVersion.patch}"
    val preReleaseVersionName = baseVersion.toString()
    val debugVersionName = preReleaseVersionName + DEBUG_SUFFIX

    val versionCode: Int
        get() {
            val version = baseVersion

            val major = version.major
            val minor = version.minor
            val patch = version.patch

            val preParts = version.preRelease?.split(".") ?: emptyList()
            val preType = preParts.getOrNull(0) ?: ""
            val preNum = preParts.getOrNull(1)?.toIntOrNull() ?: 0

            val preOffset = when (preType) {
                "" -> 3_000
                "rc" -> 2_000
                "beta" -> 1_000
                "alpha" -> 0
                else -> error("Unknown pre-release type: $preType")
            }

            return major * 10_000_000 +
                    minor * 100_000 +
                    patch * 1_000 +
                    preOffset + preNum
        }

    fun resolveVersionName(buildType: String) = when (buildType.lowercase()) {
        "debug" -> debugVersionName
        "release" -> if (baseVersion.isStable) stableVersionName else preReleaseVersionName
        else -> preReleaseVersionName
    }
}
