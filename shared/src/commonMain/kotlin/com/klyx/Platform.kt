package com.klyx

interface Platform {
    val name: String
    val os: String
    val architecture: String
}

expect val lineSeparator: String

expect fun platform(): Platform
