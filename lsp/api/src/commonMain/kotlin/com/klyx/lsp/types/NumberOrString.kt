package com.klyx.lsp.types

import kotlin.contracts.contract

typealias NumberOrString = OneOf<Int, String>

fun NumberOrString.number(): Int? {
    contract {
        returnsNotNull() implies (this@number is OneOf.Left)
    }
    return if (this is OneOf.Left) value else null
}

fun NumberOrString.string(): String? {
    contract {
        returnsNotNull() implies (this@string is OneOf.Right)
    }
    return if (isString()) value else null
}

fun NumberOrString.isNumber(): Boolean {
    contract {
        returns(true) implies (this@isNumber is OneOf.Left)
    }
    return this is OneOf.Left
}

fun NumberOrString.isString(): Boolean {
    contract {
        returns(true) implies (this@isString is OneOf.Right)
    }
    return this is OneOf.Right
}
