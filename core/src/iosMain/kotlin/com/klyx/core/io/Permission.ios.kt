package com.klyx.core.io

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
actual fun rememberStoragePermissionState(): State<Boolean> = remember { mutableStateOf(true) }
