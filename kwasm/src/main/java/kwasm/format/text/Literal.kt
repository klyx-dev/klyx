/*
 * Copyright 2019 Google LLC
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

package kwasm.format.text

import kwasm.ast.Literal
import kwasm.format.ParseException
import kwasm.format.text.token.FloatLiteral
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.StringLiteral
import kwasm.format.text.token.Token
import kotlin.reflect.KClass

/** Parses a [Literal] of type [T] from the receiving [List] of [Token]s at the given position. */
@OptIn(ExperimentalUnsignedTypes::class)
fun <T : Any> List<Token>.parseLiteral(
    fromIndex: Int,
    asClass: KClass<T>
): ParseResult<Literal<T>> {
    val node = getOrNull(fromIndex)
    val context = node?.context ?: getOrNull(fromIndex - 1)?.context

    @Suppress("UNCHECKED_CAST")
    val literalNode = when (asClass) {
        UInt::class -> {
            var literalToken = node as? IntegerLiteral.Signed
                ?: node as? IntegerLiteral.Unsigned
                ?: throw ParseException("Expected i32 (unexpected token|unknown operator)", context)
            literalToken = literalToken.toUnsigned()
            literalToken.magnitude = 32

            kwasm.ast.IntegerLiteral.U32(literalToken.value.toUInt())
        }
        Int::class -> {
            var literalToken = node as? IntegerLiteral.Signed
                ?: node as? IntegerLiteral.Unsigned
                ?: throw ParseException("Expected i32 (unexpected token|unknown operator)", context)
            literalToken = literalToken.toSigned()
            literalToken.magnitude = 32

            kwasm.ast.IntegerLiteral.S32(literalToken.value.toInt())
        }
        ULong::class -> {
            var literalToken = node as? IntegerLiteral.Signed
                ?: node as? IntegerLiteral.Unsigned
                ?: throw ParseException("Expected i64 (unexpected token|unknown operator)", context)
            literalToken = literalToken.toUnsigned()

            kwasm.ast.IntegerLiteral.U64(literalToken.value)
        }
        Long::class -> {
            var literalToken = node as? IntegerLiteral.Signed
                ?: node as? IntegerLiteral.Unsigned
                ?: throw ParseException("Expected i64 (unexpected token|unknown operator)", context)
            literalToken = literalToken.toSigned()
            literalToken.magnitude = 64

            kwasm.ast.IntegerLiteral.S64(literalToken.value)
        }
        Float::class -> {
            val literalToken = node as? FloatLiteral
                ?: (node as? IntegerLiteral.Signed)
                    ?.let { FloatLiteral("${it.sequence}", context = it.context) }
                ?: (node as? IntegerLiteral.Unsigned)
                    ?.let { FloatLiteral("${it.sequence}", context = it.context) }
                ?: throw ParseException("Expected f32 (unexpected token|unknown operator)", context)
            literalToken.magnitude = 32

            kwasm.ast.FloatLiteral.SinglePrecision(literalToken.value.toFloat())
        }
        Double::class -> {
            val literalToken = node as? FloatLiteral
                ?: (node as? IntegerLiteral.Signed)
                    ?.let { FloatLiteral("${it.sequence}", context = it.context) }
                ?: (node as? IntegerLiteral.Unsigned)
                    ?.let { FloatLiteral("${it.sequence}", context = it.context) }
                ?: throw ParseException("Expected f64 (unexpected token|unknown operator)", context)
            literalToken.magnitude = 64

            kwasm.ast.FloatLiteral.DoublePrecision(literalToken.value.toDouble())
        }
        String::class -> {
            val literalToken = node as? StringLiteral
                ?: throw ParseException("Expected String (unexpected token)", context)

            kwasm.ast.StringLiteral(literalToken.value)
        }
        else -> throw ParseException(
            "Type ${asClass.java.simpleName} is not a supported literal type",
            context
        )
    } as Literal<T>

    return ParseResult(literalNode, 1)
}
