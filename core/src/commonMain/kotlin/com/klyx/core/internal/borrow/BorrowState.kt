package com.klyx.core.internal.borrow

enum class BorrowState {
    // Object is owned and can be borrowed
    Owned,

    // Object has been moved (ownership transferred)
    Moved,

    // Object has been dropped and is no longer valid
    Dropped
}
