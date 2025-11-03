package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

inline val Boolean.mutableState
    @Composable
    get() = remember { mutableStateOf(this) }

inline val String.mutableState
    @Composable
    get() = remember { mutableStateOf(this) }

inline val Int.mutableState
    @Composable
    get() = remember { mutableIntStateOf(this) }

inline val Float.mutableState
    @Composable
    get() = remember { mutableFloatStateOf(this) }
