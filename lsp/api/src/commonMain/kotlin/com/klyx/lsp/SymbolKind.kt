package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A symbol kind.
 */
@JvmInline
@Serializable
value class SymbolKind private constructor(private val value: Int) {
    companion object {
        val File = SymbolKind(1)
        val Module = SymbolKind(2)
        val Namespace = SymbolKind(3)
        val Package = SymbolKind(4)
        val Class = SymbolKind(5)
        val Method = SymbolKind(6)
        val Property = SymbolKind(7)
        val Field = SymbolKind(8)
        val Constructor = SymbolKind(9)
        val Enum = SymbolKind(10)
        val Interface = SymbolKind(11)
        val Function = SymbolKind(12)
        val Variable = SymbolKind(13)
        val Constant = SymbolKind(14)
        val String = SymbolKind(15)
        val Number = SymbolKind(16)
        val Boolean = SymbolKind(17)
        val Array = SymbolKind(18)
        val Object = SymbolKind(19)
        val Key = SymbolKind(20)
        val Null = SymbolKind(21)
        val EnumMember = SymbolKind(22)
        val Struct = SymbolKind(23)
        val Event = SymbolKind(24)
        val Operator = SymbolKind(25)
        val TypeParameter = SymbolKind(26)

        val entries = listOf(
            File,
            Module,
            Namespace,
            Package,
            Class,
            Method,
            Property,
            Field,
            Constructor,
            Enum,
            Interface,
            Function,
            Variable,
            Constant,
            String,
            Number,
            Boolean,
            Array,
            Object,
            Key,
            Null,
            EnumMember,
            Struct,
            Event,
            Operator,
            TypeParameter,
        )
    }
}
