package com.klyx

import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

interface WorktreeDrawerState {
    val isOpen: Boolean
    val isClosed: Boolean

    suspend fun open()
    suspend fun close()
}

@Composable
fun rememberWorktreeDrawerState(
    initialValue: DrawerValue,
    onOpen: () -> Unit,
    onClose: () -> Unit
): WorktreeDrawerState {
    return remember {
        WorktreeDrawerState(
            isOpen = { initialValue == DrawerValue.Open },
            open = onOpen,
            close = onClose
        )
    }
}

private fun WorktreeDrawerState(
    isOpen: () -> Boolean,
    open: suspend () -> Unit,
    close: suspend () -> Unit
) = object : WorktreeDrawerState {
    override val isOpen: Boolean get() = isOpen()
    override val isClosed: Boolean get() = !this.isOpen
    override suspend fun open() = open()
    override suspend fun close() = close()
}
