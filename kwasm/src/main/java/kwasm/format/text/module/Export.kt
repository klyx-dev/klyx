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
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.format.ParseException
import kwasm.format.parseCheck
import kwasm.format.parseCheckNotNull
import kwasm.format.text.ParseResult
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Parses an [Export] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#exports):
 *
 * ```
 *   export_I ::= ‘(’ ‘export’ nm:name d:exportdesc_I ‘)’ => {name nm, desc d}
 * ```
 */
fun List<Token>.parseExport(
    fromIndex: Int,
    counts: TextModuleCounts,
): Pair<ParseResult<Export>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "export")) return null
    currentIndex++
    val name = parseLiteral(currentIndex, String::class)
    currentIndex += name.parseLength
    val descriptor = parseExportDescriptor(currentIndex)
    currentIndex += descriptor.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        Export(
            name.astNode.value,
            descriptor.astNode
        ),
        currentIndex - fromIndex
    ) to counts
}

/**
 * Parses an [ExportDescriptor] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#exports):
 *
 * ```
 *  exportdesc_I    ::= ‘(’ ‘func’ x:funcidx_I ‘)’      => func x
 *                      ‘(’ ‘table’ x:tableidx_I ‘)’    => table x
 *                      ‘(’ ‘memory’ x:memidx_I ‘)’     => mem x
 *                      ‘(’ ‘global’ x:globalidx_I ‘)’  => global x
 * ```
 */
fun List<Token>.parseExportDescriptor(fromIndex: Int): ParseResult<ExportDescriptor<*>> {
    var currentIndex = fromIndex
    parseCheck(contextAt(currentIndex), isOpenParen(currentIndex), "Expected '('")
    currentIndex++
    val keyword = parseCheckNotNull(
        contextAt(currentIndex),
        getOrNull(currentIndex) as? Keyword,
        "Expected 'func', 'table', 'memory', or 'global'"
    )
    currentIndex++
    val descriptor = when (keyword.value) {
        "func" -> {
            val index = parseIndex<Identifier.Function>(currentIndex)
            currentIndex += index.parseLength
            ExportDescriptor.Function(index.astNode)
        }
        "table" -> {
            val index = parseIndex<Identifier.Table>(currentIndex)
            currentIndex += index.parseLength
            ExportDescriptor.Table(index.astNode)
        }
        "memory" -> {
            val index = parseIndex<Identifier.Memory>(currentIndex)
            currentIndex += index.parseLength
            ExportDescriptor.Memory(index.astNode)
        }
        "global" -> {
            val index = parseIndex<Identifier.Global>(currentIndex)
            currentIndex += index.parseLength
            ExportDescriptor.Global(index.astNode)
        }
        else -> throw ParseException(
            "Expected 'func', 'table', 'memory', or 'global'",
            contextAt(currentIndex)
        )
    }
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(descriptor, currentIndex - fromIndex)
}
