package com.klyx.core

import kotlinx.datetime.Clock

fun generateId() = "${Clock.System.now().toEpochMilliseconds()}_${(0 .. 1000).random()}"
