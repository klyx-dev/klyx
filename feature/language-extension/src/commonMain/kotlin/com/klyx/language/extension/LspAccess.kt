package com.klyx.language.extension

sealed interface LspAccess {
    data object NoOp : LspAccess
}
