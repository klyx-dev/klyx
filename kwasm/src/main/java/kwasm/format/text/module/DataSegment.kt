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

import kwasm.ast.Identifier
import kwasm.ast.module.DataSegment
import kwasm.ast.module.Index
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.StringLiteral
import kwasm.format.text.token.Token
import java.util.stream.Collectors

/**
 * Parses a [DataSegment] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#data-segments):
 *
 * ```
 *   data_I     ::= ‘(’ ‘data’ x:memidx_I ‘(’ ‘offset’ e:expr_I ‘)’ b*:datastring ‘)’
 *                  => {data x′, offset e, init b*}
 *   datastring ::= (b*:string)*
 *                  => concat((b*)*)
 * ```
 *
 * **Note:*** In the current version of WebAssembly, the only valid memory index is `0` or a
 * symbolic memory identifier resolving to the same value.
 */
@Suppress("UNCHECKED_CAST")
fun List<Token>.parseDataSegment(
    fromIndex: Int,
    counts: TextModuleCounts,
): Pair<ParseResult<DataSegment>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "data")) return null
    currentIndex++

    val memIndex = if (!isOpenParen(currentIndex)) {
        parseIndex<Identifier.Memory>(currentIndex)
    } else {
        // Empty is okay, we just need to set it to zero.
        ParseResult(
            Index.ByInt(0) as Index<Identifier.Memory>,
            0
        )
    }
    currentIndex += memIndex.parseLength

    val offset = parseOffset(currentIndex)
    currentIndex += offset.parseLength

    val (dataStringBytes, parseLength) = parseDataString(currentIndex)
    currentIndex += parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        DataSegment(
            memIndex.astNode,
            offset.astNode,
            dataStringBytes
        ),
        currentIndex - fromIndex
    ) to counts
}

internal fun List<Token>.parseDataString(fromIndex: Int): Pair<ByteArray, Int> {
    var currentIndex = fromIndex
    val strings = StringBuilder()
    while (true) {
        val currentToken = getOrNull(currentIndex)
        if (currentToken is StringLiteral) {
            strings.append(currentToken.value)
            currentIndex++
        } else break
    }
    val bytes = strings.codePoints().mapToObj { it.toByte() }
        .collect(Collectors.toList())
    val byteArray = ByteArray(bytes.size) { bytes[it] }
    return byteArray to (currentIndex - fromIndex)
}
