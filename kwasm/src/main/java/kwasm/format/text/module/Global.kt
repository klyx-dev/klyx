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

import kwasm.ast.AstNode
import kwasm.ast.AstNodeList
import kwasm.ast.Identifier
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Global
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.format.parseCheck
import kwasm.format.parseCheckNotNull
import kwasm.format.text.ParseResult
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.contextAt
import kwasm.format.text.instruction.parseExpression
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseLiteral
import kwasm.format.text.parseOrCreateIdentifier
import kwasm.format.text.token.Token
import kwasm.format.text.tokensUntilParenClosure
import kwasm.format.text.type.parseGlobalType

/**
 * Parses a [Global] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#globals):
 *
 * ```
 *   global_I   ::= ‘(’ ‘global’ id? gt:globaltype  e:expr_I ‘)’    => {type gt, init e}
 * ```
 */
fun List<Token>.parseGlobal(
    fromIndex: Int,
    counts: TextModuleCounts,
): Pair<ParseResult<Global>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "global")) return null
    currentIndex++

    val (id, updatedCounts) = parseOrCreateIdentifier<Identifier.Global>(currentIndex, counts)
    currentIndex += id.parseLength

    val globalType = parseGlobalType(currentIndex)
    currentIndex += globalType.parseLength

    val expression = parseExpression(currentIndex)
    currentIndex += expression.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        Global(
            id.astNode,
            globalType.astNode,
            expression.astNode
        ),
        currentIndex - fromIndex
    ) to updatedCounts
}

/**
 * Parses an inline [Global]-[Import] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-global-abbrev):
 *
 * ```
 *   ‘(’ ‘global’ id? ‘(’ ‘import’ name^1 name^2 ‘)’ globaltype ‘)’
 *      == ‘(’ ‘import’ name^1 name^2 ‘(’ ‘global’ id? globaltype ‘)’ ‘)’
 * ```
 */
fun List<Token>.parseInlineGlobalImport(
    fromIndex: Int,
    counts: TextModuleCounts,
): Pair<ParseResult<Import>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "global")) return null
    currentIndex++

    val (id, updatedCounts) = parseOrCreateIdentifier<Identifier.Global>(currentIndex, counts)
    currentIndex += id.parseLength

    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "import")) return null
    currentIndex++

    val moduleName = parseLiteral(currentIndex, String::class)
    currentIndex += moduleName.parseLength
    val tableName = parseLiteral(currentIndex, String::class)
    currentIndex += tableName.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val globalType = parseGlobalType(currentIndex)
    currentIndex += globalType.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        Import(
            moduleName.astNode.value,
            tableName.astNode.value,
            ImportDescriptor.Global(
                id.astNode,
                globalType.astNode
            )
        ),
        currentIndex - fromIndex
    ) to updatedCounts
}

/**
 * Parses an inline [Global]-[Export] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-global-abbrev):
 *
 * ```
 *   ‘(’ ‘global’ id? ‘(’ ‘export’ name ‘)’ ... ‘)’
 *      == ‘(’ ‘export’ name ‘(’ ‘global’ id′ ‘)’ ‘)’ ‘(’ ‘global’ id′ ... ‘)’
 *          (if id′ = id? != ϵ ∨ id′ fresh)
 * ```
 *
 * Where “...” can contain another import or export.
 */
@Suppress("UNCHECKED_CAST")
fun List<Token>.parseInlineGlobalExport(
    fromIndex: Int,
    counts: TextModuleCounts,
): Pair<ParseResult<AstNodeList<AstNode>>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "global")) return null
    currentIndex++

    val (id, updatedCounts) = parseOrCreateIdentifier<Identifier.Global>(currentIndex, counts)
    currentIndex += id.parseLength

    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "export")) return null
    currentIndex++

    val exportName = parseLiteral(currentIndex, String::class)
    currentIndex += exportName.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val result = mutableListOf<AstNode>(
        Export(
            exportName.astNode.value,
            ExportDescriptor.Global(
                Index.ByInt(updatedCounts.globals - 1) as Index<Identifier.Global>
            )
        )
    )

    if (isClosedParen(currentIndex)) {
        // No `...` case, so we can return here.
        currentIndex++
        return ParseResult(
            AstNodeList(result),
            currentIndex - fromIndex
        ) to updatedCounts
    }

    // Looks like there might be more to handle, so construct a new list of tokens to recurse with.
    // Add the opening paren and 'global' keyword.
    val withoutFirstExport = mutableListOf(
        this[fromIndex],
        this[fromIndex + 1]
    )

    // Add the id.
    if (id.parseLength > 0) {
        (0 until id.parseLength).forEach {
            withoutFirstExport.add(this[fromIndex + 2 + it])
        }
    }

    val lengthOfPrefix = withoutFirstExport.size

    // Add all of the tokens until the closing paren for the global block.
    withoutFirstExport.addAll(tokensUntilParenClosure(currentIndex, expectedClosures = 1))

    // Recurse.
    val (additionalItems, _) = parseCheckNotNull(
        contextAt(currentIndex),
        withoutFirstExport.parseInlineGlobalExport(0, counts)
            ?: withoutFirstExport.parseInlineGlobalImport(0, counts)
            ?: withoutFirstExport.parseGlobal(0, counts),
        "Expected additional details for global"
    )
    currentIndex += (additionalItems.parseLength - lengthOfPrefix)

    when (val node = additionalItems.astNode) {
        is AstNodeList<*> -> result.addAll(node)
        else -> result.add(node)
    }

    return ParseResult(
        AstNodeList(result),
        currentIndex - fromIndex
    ) to updatedCounts
}
