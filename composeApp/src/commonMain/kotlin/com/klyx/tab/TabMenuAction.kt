package com.klyx.tab

import kotlin.jvm.JvmInline

sealed interface TabMenuAction {
    @JvmInline
    value class Close(val index: Int) : TabMenuAction

    @JvmInline
    value class CloseOthers(val currentIndex: Int) : TabMenuAction

    @JvmInline
    value class CloseLeft(val currentIndex: Int) : TabMenuAction

    @JvmInline
    value class CloseRight(val currentIndex: Int) : TabMenuAction

    @JvmInline
    value class CloseAll(val currentIndex: Int) : TabMenuAction

    @JvmInline
    value class CopyPath(val currentIndex: Int) : TabMenuAction

    @JvmInline
    value class CopyRelativePath(val currentIndex: Int) : TabMenuAction
}
