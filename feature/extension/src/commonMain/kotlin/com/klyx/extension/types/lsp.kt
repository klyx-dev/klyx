package com.klyx.extension.types

import com.klyx.extension.native.Completion
import com.klyx.extension.native.CompletionKind
import com.klyx.extension.native.CompletionLabelDetails
import com.klyx.extension.native.InsertTextFormat
import com.klyx.extension.native.Symbol
import com.klyx.extension.native.SymbolKind

/**
 * An LSP completion.
 */
typealias Completion = Completion

/**
 * The kind of an LSP completion.
 */
typealias CompletionKind = CompletionKind

/**
 * Label details for an LSP completion.
 */
typealias CompletionLabelDetails = CompletionLabelDetails

/**
 * Defines how to interpret the insert text in a completion item.
 */
typealias InsertTextFormat = InsertTextFormat

/**
 * An LSP symbol.
 */
typealias Symbol = Symbol

/**
 * The kind of an LSP symbol.
 */
typealias SymbolKind = SymbolKind
