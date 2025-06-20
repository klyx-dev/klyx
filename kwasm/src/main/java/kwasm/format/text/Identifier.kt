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

import kwasm.ast.Identifier
import kwasm.format.ParseException
import kwasm.format.parseCheckNotNull
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.Token

/**
 * Parses an [Identifier] of type [T] from the receiving [List] of [Token]s. If no explicit
 * [Identifier] is found, an implicit one with [Identifier.stringRepr] and [Identifier.unique] both
 * set to `null` is returned.
 *
 * **Note:** Does not support [Identifier.TypeDef].
 */
inline fun <reified T : Identifier> List<Token>.parseIdentifier(
    fromIndex: Int,
    intAllowed: Boolean = false
): ParseResult<T>? {
    var currentIndex = fromIndex
    val maybeId = getOrNull(currentIndex) as? kwasm.format.text.token.Identifier
    val maybeInt = getOrNull(currentIndex) as? IntegerLiteral<*>

    val id = if (maybeId != null) {
        currentIndex++
        createIdentifier<T>(maybeId.value)
    } else if (maybeInt != null && intAllowed) {
        currentIndex++
        parseCheckNotNull(
            contextAt(currentIndex - 1),
            createIdentifier<T>(intValue = maybeInt.toSigned().value.toInt())
                ?.takeIf { it.unique != null && (it.unique as Int) >= 0 },
            "Identifier must not be negative"
        )
    } else null
    return id?.let { ParseResult(it, currentIndex - fromIndex) }
}

inline fun <reified T : Identifier> List<Token>.parseOrCreateIdentifier(
    fromIndex: Int,
    counts: TextModuleCounts
): Pair<ParseResult<T>, TextModuleCounts> {
    val parsed = parseIdentifier<T>(fromIndex, false)

    val value = when (T::class) {
        Identifier.Global::class -> counts.globals
        Identifier.Memory::class -> counts.memories
        Identifier.Table::class -> counts.tables
        Identifier.Function::class -> counts.functions
        Identifier.Type::class -> counts.types
        else -> null
    }
    val identifier = createIdentifier<T>(stringValue = null, intValue = value)
    return if (parsed == null && identifier != null) {
        ParseResult(identifier, 0) to counts.incrementFor(identifier)
    } else if (parsed != null) {
        parsed to counts.incrementFor(parsed.astNode)
    } else throw ParseException("Unsupported identifier found", contextAt(fromIndex))
}

/**
 * Creates an [Identifier] of type [T].
 */
inline fun <reified T : Identifier> createIdentifier(
    stringValue: String? = null,
    intValue: Int? = "$stringValue".hashCode()
): T? {
    return when (T::class) {
        Identifier.Global::class -> Identifier.Global(stringValue, intValue)
        Identifier.Memory::class -> Identifier.Memory(stringValue, intValue)
        Identifier.Table::class -> Identifier.Table(stringValue, intValue)
        Identifier.Function::class -> Identifier.Function(stringValue, intValue)
        Identifier.Type::class -> Identifier.Type(stringValue, intValue)
        Identifier.Local::class -> Identifier.Local(stringValue, intValue)
        Identifier.Label::class -> Identifier.Label(stringValue, intValue)
        else -> null // TypeDef not supported here.
    } as T?
}
