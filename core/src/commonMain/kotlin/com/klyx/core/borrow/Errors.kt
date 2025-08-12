package com.klyx.core.borrow

sealed class BorrowError(message: String) : RuntimeException(message)
class UseAfterDropError(message: String) : BorrowError(message)
class DoubleBorrowError(message: String) : BorrowError(message)
class BorrowWhileMovedError(message: String) : BorrowError(message)
class InvalidPointerError(message: String) : BorrowError(message)

fun useAfterDropError(message: String): Nothing = throw UseAfterDropError(message)
fun doubleBorrowError(message: String): Nothing = throw DoubleBorrowError(message)
fun borrowWhileMovedError(message: String): Nothing = throw BorrowWhileMovedError(message)
fun invalidPointerError(message: String): Nothing = throw InvalidPointerError(message)
