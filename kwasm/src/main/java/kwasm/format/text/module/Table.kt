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
import kwasm.ast.IntegerLiteral
import kwasm.ast.astNodeListOf
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.ElementSegment
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.ast.module.Offset
import kwasm.ast.module.Table
import kwasm.ast.type.ElementType
import kwasm.ast.type.Limits
import kwasm.ast.type.TableType
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
import kwasm.format.text.parseOrCreateIdentifier
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token
import kwasm.format.text.tokensUntilParenClosure
import kwasm.format.text.type.parseTableType

/**
 * Parses a [Table] and (optionally) an [ElementType] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#tables):
 *
 * ```
 *   table_I ::= ‘(’ ‘table’ id? tt:tabletype ‘)’ => {type tt}
 * ```
 *
 * **Abbreviations**
 *
 * An element segment can be given inline with a table definition, in which case its offset is `0`
 * and the limits of the table type are inferred from the length of the given segment:
 *
 * ```
 *   ‘(’ ‘table’ id? elemtype ‘(’ ‘elem’ xn:vec(funcidx) ‘)’ ‘)’
 *      == ‘(’ ‘table’ id′ n n elemtype ‘)’ ‘(’ ‘elem’ id′ ‘(’ ‘i32.const’ ‘0’ ‘)’ vec(funcidx) ‘)’
 *          (if id′ == id? != ϵ ∨ id′ fresh)
 * ```
 */
fun List<Token>.parseTable(
    fromIndex: Int,
    counts: TextModuleCounts
): Pair<ParseResult<AstNodeList<AstNode>>, TextModuleCounts>? {
    parseTableBasic(fromIndex, counts)?.let { (result, updatedCounts) ->
        return ParseResult<AstNodeList<AstNode>>(
            astNodeListOf(result.astNode),
            result.parseLength
        ) to updatedCounts
    }
    return parseTableAndElementSegment(fromIndex, counts)
}

/** See [parseTable]. */
internal fun List<Token>.parseTableBasic(
    fromIndex: Int,
    counts: TextModuleCounts
): Pair<ParseResult<Table>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "table")) return null
    currentIndex++
    val (id, updatedCounts) = parseOrCreateIdentifier<Identifier.Table>(currentIndex, counts)
    currentIndex += id.parseLength
    val tableType = try { parseTableType(currentIndex) } catch (e: ParseException) { null }
        ?: return null
    currentIndex += tableType.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        Table(id.astNode, tableType.astNode),
        currentIndex - fromIndex
    ) to updatedCounts
}

/** See [parseTable]. */
@Suppress("UNCHECKED_CAST")
internal fun List<Token>.parseTableAndElementSegment(
    fromIndex: Int,
    counts: TextModuleCounts
): Pair<ParseResult<AstNodeList<AstNode>>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "table")) return null
    currentIndex++
    val (id, updatedCounts) = parseOrCreateIdentifier<Identifier.Table>(currentIndex, counts)
    currentIndex += id.parseLength

    val keyword = getOrNull(currentIndex) as? Keyword ?: return null
    val elemType = when (keyword.value) {
        "funcref" -> ElementType.FunctionReference
        else -> return null
    }
    currentIndex++

    parseCheck(
        contextAt(currentIndex),
        isOpenParen(currentIndex),
        "Expected inline element segment"
    )
    currentIndex++
    parseCheck(
        contextAt(currentIndex),
        isKeyword(currentIndex, "elem"),
        "Expected inline element segment"
    )
    currentIndex++

    val funcIndices = parseIndices<Identifier.Function>(currentIndex)
    currentIndex += funcIndices.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val (identifier, elementSegmentIndex) = if (id.astNode.stringRepr != null) {
        id.astNode to (Index.ByInt(updatedCounts.tables - 1) as Index<Identifier.Table>)
    } else {
        val indexNum = parseCheckNotNull(contextAt(fromIndex), id.astNode.unique)
        id.astNode to (Index.ByInt(indexNum) as Index<Identifier.Table>)
    }

    return ParseResult(
        astNodeListOf(
            Table(
                identifier,
                TableType(
                    Limits(
                        funcIndices.astNode.size.toLong(),
                        funcIndices.astNode.size.toLong()
                    ),
                    elemType
                )
            ),
            ElementSegment(
                elementSegmentIndex,
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                funcIndices.astNode
            )
        ),
        currentIndex - fromIndex
    ) to updatedCounts
}

/**
 * Parses an inline [Table]-[Import] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-table-abbrev):
 *
 * ```
 *   ‘(’ ‘table’ id? ‘(’ ‘import’ name^1 name^2 ‘)’ tabletype ‘)’
 *      == ‘(’ ‘import’ name^1 name^2 ‘(’ ‘table’ id? tabletype ‘)’ ‘)’
 * ```
 */
fun List<Token>.parseInlineTableImport(
    fromIndex: Int,
    counts: TextModuleCounts
): Pair<ParseResult<Import>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "table")) return null
    currentIndex++

    val (identifier, updatedCounts) =
        parseOrCreateIdentifier<Identifier.Table>(currentIndex, counts)
    currentIndex += identifier.parseLength

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

    val tableType = parseTableType(currentIndex)
    currentIndex += tableType.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        Import(
            moduleName.astNode.value,
            tableName.astNode.value,
            ImportDescriptor.Table(
                identifier.astNode.takeIf { it.stringRepr != null }
                    ?: Identifier.Table(null, null),
                tableType.astNode
            )
        ),
        currentIndex - fromIndex
    ) to updatedCounts
}

/**
 * Parses an inline [Table]-[Export] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-table-abbrev):
 *
 * ```
 *   ‘(’ ‘table’ id? ‘(’ ‘export’ name ‘)’ ... ‘)’
 *      == ‘(’ ‘export’ name ‘(’ ‘table’ id′ ‘)’ ‘)’ ‘(’ ‘table’ id′ ... ‘)’
 *          (if id′ = id? != ϵ ∨ id′ fresh)
 * ```
 *
 * Where “...” can contain another import or export or an inline elements segment.
 */
@Suppress("UNCHECKED_CAST")
fun List<Token>.parseInlineTableExport(
    fromIndex: Int,
    counts: TextModuleCounts
): Pair<ParseResult<AstNodeList<AstNode>>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "table")) return null
    currentIndex++

    val (identifier, updatedCounts) =
        parseOrCreateIdentifier<Identifier.Table>(currentIndex, counts)
    currentIndex += identifier.parseLength

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
            ExportDescriptor.Table(
                Index.ByInt(updatedCounts.tables - 1) as Index<Identifier.Table>
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
    // Add the opening paren and 'table' keyword.
    val withoutFirstExport = mutableListOf(
        this[fromIndex],
        this[fromIndex + 1]
    )

    // Add the id.
    if (identifier.parseLength > 0) {
        (0 until identifier.parseLength).forEach {
            withoutFirstExport.add(this[fromIndex + 2 + it])
        }
    }

    val lengthOfPrefix = withoutFirstExport.size

    // Add all of the tokens until the closing paren for the table block.
    withoutFirstExport.addAll(tokensUntilParenClosure(currentIndex, expectedClosures = 1))

    // Recurse.
    val (additionalItems, _) = parseCheckNotNull(
        contextAt(currentIndex),
        withoutFirstExport.parseInlineTableExport(0, counts)
            ?: withoutFirstExport.parseInlineTableImport(0, counts)
            ?: withoutFirstExport.parseTable(0, counts),
        "Expected additional details for table"
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
