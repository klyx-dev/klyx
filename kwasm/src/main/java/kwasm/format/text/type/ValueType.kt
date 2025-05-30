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

package kwasm.format.text.type

import kwasm.ast.AstNodeList
import kwasm.ast.type.ValueType
import kwasm.format.parseCheck
import kwasm.format.parseCheckNotNull
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isKeyword
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Parses a ValueType from a list of Tokens.
 * From [the docs](https://webassembly.github.io/spec/core/text/types.html#value-types):
 *
 * ```
 *   valtype ::=  { 'i32' -> I32
 *                  'i64' -> I64
 *                  'f32' -> F32
 *                  'f64' -> F64 }
 * ```
 */
fun List<Token>.parseValueType(currentIndex: Int): ParseResult<ValueType> {
    parseCheckNotNull(
        contextAt(currentIndex),
        getOrNull(currentIndex) as? Keyword,
        "Invalid ValueType: Expecting keyword token"
    )
    return parseCheckNotNull(
        contextAt(currentIndex),
        parseOptionalValueType(currentIndex),
        "Invalid ValueType: Expecting i32, i64, f32, or f64"
    )
}

/** Similar to [parseValueType], but does not throw if a [ValueType] is `null`. */
fun List<Token>.parseOptionalValueType(currentIndex: Int): ParseResult<ValueType>? {
    val valueType = when {
        isKeyword(currentIndex, "i32") -> ValueType.I32
        isKeyword(currentIndex, "i64") -> ValueType.I64
        isKeyword(currentIndex, "f32") -> ValueType.F32
        isKeyword(currentIndex, "f64") -> ValueType.F64
        else -> null
    }
    return valueType?.let { ParseResult(it, 1) }
}

/**
 * Parses an [AstNodeList] of [ValueType]s from the receiving [List] of [Token]s.
 *
 * See [parseValueType].
 */
fun List<Token>.parseValueTypes(
    fromIndex: Int,
    minRequired: Int = 0,
    maxAllowed: Int = Int.MAX_VALUE
): ParseResult<AstNodeList<ValueType>> {
    var currentIndex = fromIndex
    val types = mutableListOf<ValueType>()

    while (true) {
        val type = parseOptionalValueType(currentIndex) ?: break
        currentIndex += type.parseLength
        types.add(type.astNode)
    }

    parseCheck(
        contextAt(currentIndex),
        types.size >= minRequired,
        "Not enough ValueTypes, min = $minRequired"
    )
    parseCheck(
        contextAt(currentIndex),
        types.size <= maxAllowed,
        "Too many ValueTypes, max = $maxAllowed"
    )

    return ParseResult(
        AstNodeList(types),
        currentIndex - fromIndex
    )
}
