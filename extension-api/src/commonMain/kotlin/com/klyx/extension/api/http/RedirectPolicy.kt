package com.klyx.extension.api.http

sealed interface RedirectPolicy {
    data object NoFollow : RedirectPolicy
    data class FollowLimit(val limit: Int) : RedirectPolicy
    data object FollowAll : RedirectPolicy
}

internal fun parseRedirectPolicy(tag: Int, limit: Int) = when (tag) {
    0 -> RedirectPolicy.NoFollow
    1 -> RedirectPolicy.FollowLimit(limit)
    2 -> RedirectPolicy.FollowAll
    else -> error("Unknown redirect policy: $tag")
}
