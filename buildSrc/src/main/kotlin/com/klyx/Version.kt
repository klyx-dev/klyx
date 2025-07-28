package com.klyx

object Version {
    const val STABLE_VERSION_NAME = "1.7.0"
    const val PRE_RELEASE_VERSION_SUFFIX = "-beta"
    const val DEBUG_VERSION_SUFFIX = "-dev"

    val VERSION_NAME = "1.7.0-beta.0"
    val AppVersionCode get() = calculateVersionCode()

    private fun calculateVersionCode(): Int {
        val semverRegex = Regex("""(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta|rc)\.(\d+))?""")
        val match = semverRegex.matchEntire(VERSION_NAME) ?: error("Invalid semver format: $VERSION_NAME")

        val (majorStr, minorStr, patchStr, preReleaseType, preReleaseNumStr) = match.destructured

        val major = majorStr.toInt()
        val minor = minorStr.toInt()
        val patch = patchStr.toInt()
        val preReleaseNum = preReleaseNumStr.toIntOrNull() ?: 0

        val preReleaseOffset = when (preReleaseType) {
            "" -> 3_000 // stable
            "rc" -> 2_000
            "beta" -> 1_000
            "alpha" -> 0
            else -> error("Unknown pre-release type: $preReleaseType")
        }

        return major * 10_000_000 +
                minor * 100_000 +
                patch * 1_000 +
                preReleaseOffset + preReleaseNum
    }
}
