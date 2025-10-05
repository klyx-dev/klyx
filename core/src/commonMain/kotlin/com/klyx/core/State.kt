package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

inline val Boolean.state
    @Composable
    get() = remember { mutableStateOf(this) }

inline val String.state
    @Composable
    get() = remember { mutableStateOf(this) }

inline val Int.state
    @Composable
    get() = remember { mutableIntStateOf(this) }

inline val Float.state
    @Composable
    get() = remember { mutableFloatStateOf(this) }
