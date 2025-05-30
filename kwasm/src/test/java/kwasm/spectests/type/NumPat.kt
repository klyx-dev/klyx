/*
 * Copyright 2021 Google LLC
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

package kwasm.spectests.type

import kwasm.ast.AstNode
import kwasm.ast.Literal
import kwasm.format.text.ParseResult
import kwasm.format.text.isKeyword
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import java.text.ParseException

sealed class NumPat : AstNode {
    data class Number(val value: Literal<*>) : NumPat()
    object NanCanonical : NumPat()
    object NanArithmetic : NumPat()
}

fun List<Token>.parseNumPat(fromIndex: Int): ParseResult<out NumPat>? {
    if (isKeyword(fromIndex, "nan:canonical")) return ParseResult(NumPat.NanCanonical, 1)
    if (isKeyword(fromIndex, "nan:arithmetic")) return ParseResult(NumPat.NanArithmetic, 1)

    val num = try {
        NumPat.Number(parseLiteral(fromIndex, Int::class).astNode)
    } catch (e: ParseException) {
        if ("i32" !in e.message ?: "") return null
        try {
            NumPat.Number(parseLiteral(fromIndex, Long::class).astNode)
        } catch (e: ParseException) {
            if ("i64" !in e.message ?: "") return null
            try {
                NumPat.Number(parseLiteral(fromIndex, Float::class).astNode)
            } catch (e: ParseException) {
                if ("f32" !in e.message ?: "") return null
                NumPat.Number(parseLiteral(fromIndex, Double::class).astNode)
            }
        }
    }
    return ParseResult(num, 1)
}
