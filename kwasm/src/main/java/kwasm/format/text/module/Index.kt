/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kwasm.format.text.module

import kwasm.ast.AstNodeList
import kwasm.ast.module.Index
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.token.Identifier
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.Token
import kwasm.format.text.type.parseFunctionType

/**
 * Parses an [Index] from the receiving [List] of [Token]s.
 *
 * For details on the grammar, see [Index].
 */
@OptIn(ExperimentalUnsignedTypes::class)
inline fun <reified T : kwasm.ast.Identifier> List<Token>.parseIndex(
    fromIndex: Int
): ParseResult<out Index<T>> = when (val token = getOrNull(fromIndex)) {
    is IntegerLiteral.Unsigned -> {
        token.magnitude = 32
        @Suppress("UNCHECKED_CAST")
        (
            ParseResult(
                Index.ByInt(token.value.toInt()) as Index<T>,
                1
            )
            )
    }
    is Identifier -> {
        val id = when (T::class) {
            kwasm.ast.Identifier.Type::class -> kwasm.ast.Identifier.Type(token.value)
            kwasm.ast.Identifier.Function::class -> kwasm.ast.Identifier.Function(token.value)
            kwasm.ast.Identifier.Table::class -> kwasm.ast.Identifier.Table(token.value)
            kwasm.ast.Identifier.Memory::class -> kwasm.ast.Identifier.Memory(token.value)
            kwasm.ast.Identifier.Global::class -> kwasm.ast.Identifier.Global(token.value)
            kwasm.ast.Identifier.Local::class -> kwasm.ast.Identifier.Local(token.value)
            kwasm.ast.Identifier.Label::class -> kwasm.ast.Identifier.Label(token.value)
            else -> throw ParseException("Invalid identifier type expected", token.context)
        }

        ParseResult(Index.ByIdentifier(id as T), 1)
    }
    else -> {
        try {
            val functionType = parseFunctionType(fromIndex)
            ParseResult(
                Index.ByIdentifier(kwasm.ast.Identifier.TypeDef(functionType.astNode) as T),
                functionType.parseLength
            )
        } catch (e: ParseException) {
            if (T::class == kwasm.ast.Identifier.TypeDef::class) throw e
            throw ParseException("Expected an index (unknown operator)", contextAt(fromIndex))
        }
    }
}

/**
 * Parses up to [max] [Index]es from the receiving [List] of [Token]s starting at [fromIndex].
 * If no indices are found, returns a [ParseResult] with an empty [AstNodeList].
 *
 * @throws ParseException if fewer than [min] [Index]es are found.
 */
inline fun <reified T : kwasm.ast.Identifier> List<Token>.parseIndices(
    fromIndex: Int,
    min: Int = 0,
    max: Int = Int.MAX_VALUE
): ParseResult<AstNodeList<out Index<T>>> {
    if (fromIndex !in indices) {
        if (min == 0) return ParseResult(
            AstNodeList(emptyList()),
            0
        )
        throw ParseException(
            "Expected at least $min ${if (min > 1) "indices" else "index"}, found 0",
            getOrNull(fromIndex - 1)?.context
        )
    }

    var indicesFound = 0
    var tokensRead = 0
    val indices = mutableListOf<Index<T>>()

    while (indicesFound < max && fromIndex + tokensRead < size) {
        try {
            val index = parseIndex<T>(fromIndex + tokensRead)
            indices += index.astNode
            tokensRead += index.parseLength
            indicesFound++
        } catch (e: ParseException) { break }
    }

    if (indices.size < min) {
        throw ParseException(
            "Expected at least $min ${if (min > 1) "indices" else "index"}, found ${indices.size}",
            getOrNull(fromIndex)?.context
        )
    }

    return ParseResult(AstNodeList(indices), tokensRead)
}
