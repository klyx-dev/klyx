package com.klyx.extension.api

typealias EnvVars = List<Pair<String, String>>

/**
 * @property start The start of the range (inclusive).
 * @property end The end of the range (exclusive).
 */
data class Range(val start: Int, val end: Int)
