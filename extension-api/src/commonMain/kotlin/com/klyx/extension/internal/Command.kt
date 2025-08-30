package com.klyx.extension.internal

data class Command(
    val command: String,
    val args: List<String>,
    val env: Map<String, String>
)
