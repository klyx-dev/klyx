package com.klyx

import io.github.z4kn4fein.semver.Version

object AppVersioning {
    private val baseVersion = Version(
        major = 2,
        minor = 0,
        patch = 0,
        //preRelease = "alpha01"
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

            val preRelease = version.preRelease ?: ""

            val regex = Regex("""([a-zA-Z]+)(\d+)?""")
            val match = regex.matchEntire(preRelease)

            val preType = match?.groups?.get(1)?.value ?: ""
            val preNum = match?.groups?.get(2)?.value?.toIntOrNull() ?: 0

            val preOffset = when (preType.lowercase()) {
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
