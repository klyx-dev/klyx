package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The kind of a completion entry.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionItemKind)
 */
@JvmInline
@Serializable
value class CompletionItemKind private constructor(private val value: Int) {
    val name: String
        get() = when (this) {
            Text -> "Text"
            Method -> "Method"
            Function -> "Function"
            Constructor -> "Constructor"
            Field -> "Field"
            Variable -> "Variable"
            Class -> "Class"
            Interface -> "Interface"
            Module -> "Module"
            Property -> "Property"
            Unit -> "Unit"
            Value -> "Value"
            Enum -> "Enum"
            Keyword -> "Keyword"
            Snippet -> "Snippet"
            Color -> "Color"
            File -> "File"
            Reference -> "Reference"
            Folder -> "Folder"
            EnumMember -> "EnumMember"
            Constant -> "Constant"
            Struct -> "Struct"
            Event -> "Event"
            Operator -> "Operator"
            TypeParameter -> "TypeParameter"
            else -> "Unknown"
        }

    companion object {
        val Text = CompletionItemKind(1)
        val Method = CompletionItemKind(2)
        val Function = CompletionItemKind(3)
        val Constructor = CompletionItemKind(4)
        val Field = CompletionItemKind(5)
        val Variable = CompletionItemKind(6)
        val Class = CompletionItemKind(7)
        val Interface = CompletionItemKind(8)
        val Module = CompletionItemKind(9)
        val Property = CompletionItemKind(10)
        val Unit = CompletionItemKind(11)
        val Value = CompletionItemKind(12)
        val Enum = CompletionItemKind(13)
        val Keyword = CompletionItemKind(14)
        val Snippet = CompletionItemKind(15)
        val Color = CompletionItemKind(16)
        val File = CompletionItemKind(17)
        val Reference = CompletionItemKind(18)
        val Folder = CompletionItemKind(19)
        val EnumMember = CompletionItemKind(20)
        val Constant = CompletionItemKind(21)
        val Struct = CompletionItemKind(22)
        val Event = CompletionItemKind(23)
        val Operator = CompletionItemKind(24)
        val TypeParameter = CompletionItemKind(25)
    }
}
