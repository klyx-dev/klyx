package com.klyx.buildlogic

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.withoutSuffixes

private val currentVersion = Version(2, 0, 0, preRelease = null, buildMetadata = "preview")

private val currentVersionCode by lazy {
    val major = currentVersion.major
    val minor = currentVersion.minor
    val patch = currentVersion.patch
    val preRelease = currentVersion.preRelease ?: ""

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

    major * 10_000_000 + minor * 100_000 + patch * 1_000 + preOffset + preNum
}

fun currentVersion() = currentVersion.toString()
fun currentStableVersion() = currentVersion.withoutSuffixes().toString()
fun currentVersionCode() = currentVersionCode

