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
import kwasm.ast.module.DataSegment
import kwasm.ast.module.ElementSegment
import kwasm.ast.module.Export
import kwasm.ast.module.Global
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Memory
import kwasm.ast.module.StartFunction
import kwasm.ast.module.Table
import kwasm.ast.module.Type
import kwasm.ast.module.WasmFunction
import kwasm.ast.module.WasmModule
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.contextAt
import kwasm.format.text.instruction.parseLabel
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Token

/**
 * Parses a [WasmModule] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-module):
 *
 * A module consists of a sequence of fields that can occur in any order. All definitions and their
 * respective bound identifiers scope over the entire module, including the text preceding them.
 *
 * A module may optionally bind an identifier that names the module. The name serves a documentary
 * role only.
 *
 * ```
 *   module         ::= ‘(’ ‘module’ id? (m:modulefield_I)* ‘)’     => ⨁m*
 *                      (if I = ⨁ idc(modulefield)* well-formed)
 *   modulefield_I  ::= ty:type                                     => {types ty}
 *                      im:import_I                                 => {imports im}
 *                      fn:func_I                                   => {funcs fn}
 *                      ta:table_I                                  => {tables ta}
 *                      me:mem_I                                    => {mems me}
 *                      gl:global_I                                 => {globals gl}
 *                      ex:export_I                                 => {exports ex}
 *                      st:start_I                                  => {start st}
 *                      el:elem_I                                   => {elem el}
 *                      da:data_I                                   => {data da}
 * ```
 */
fun List<Token>.parseModule(fromIndex: Int): ParseResult<WasmModule>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "module")) return null
    currentIndex++
    val label = parseLabel(currentIndex)
    currentIndex += label.parseLength

    val allNodes = mutableListOf<AstNode>()

    var counts = TextModuleCounts(0, 0, 0, 0, 0)
    while (true) {
        val (parseResult, updatedCounts) = parseType(currentIndex, counts)
            ?: parseImport(currentIndex, counts)
            ?: parseInlineWasmFunctionImport(currentIndex, counts)
            ?: parseInlineWasmFunctionExport(currentIndex, counts)
            ?: parseWasmFunction(currentIndex, counts)
            ?: parseInlineTableImport(currentIndex, counts)
            ?: parseInlineTableExport(currentIndex, counts)
            ?: parseTable(currentIndex, counts)
            ?: parseInlineMemoryImport(currentIndex, counts)
            ?: parseInlineMemoryExport(currentIndex, counts)
            ?: parseMemory(currentIndex, counts)
            ?: parseInlineGlobalImport(currentIndex, counts)
            ?: parseInlineGlobalExport(currentIndex, counts)
            ?: parseGlobal(currentIndex, counts)
            ?: parseExport(currentIndex, counts)
            ?: parseStartFunction(currentIndex, counts)
            ?: parseElementSegment(currentIndex, counts)
            ?: parseDataSegment(currentIndex, counts)
            ?: break
        when (val node = parseResult.astNode) {
            is AstNodeList<*> -> allNodes.addAll(node)
            else -> allNodes.add(node)
        }
        currentIndex += parseResult.parseLength
        counts = updatedCounts
    }

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val types = mutableListOf<Type>()
    val imports = mutableListOf<Import>()
    val functions = mutableListOf<WasmFunction>()
    val tables = mutableListOf<Table>()
    val memories = mutableListOf<Memory>()
    val globals = mutableListOf<Global>()
    val exports = mutableListOf<Export>()
    var startFunction: StartFunction? = null
    val elementSegments = mutableListOf<ElementSegment>()
    val dataSegments = mutableListOf<DataSegment>()

    allNodes.forEach {
        when (it) {
            is Type -> types.add(it)
            is Import -> {
                if (it.descriptor is ImportDescriptor.Memory) {
                    parseCheck(
                        contextAt(fromIndex),
                        memories.find { prior -> prior.id == it.descriptor.id } == null &&
                            imports.filter { import ->
                            import.descriptor is ImportDescriptor.Memory
                        }.find { des -> des.descriptor.id == it.descriptor.id } == null,
                        "Modules may only define one memory for each id (duplicate memory)"
                    )
                }
                imports.add(it)
            }
            is WasmFunction -> functions.add(it)
            is Table -> tables.add(it)
            is Memory -> {
                parseCheck(
                    contextAt(fromIndex),
                    memories.find { prior -> prior.id == it.id } == null &&
                        imports.filter { import -> import.descriptor is ImportDescriptor.Memory }
                        .find { des -> des.descriptor.id == it.id } == null,
                    "Modules may only define one memory for each id (duplicate memory)"
                )
                memories.add(it)
            }
            is Global -> globals.add(it)
            is Export -> exports.add(it)
            is StartFunction -> {
                parseCheck(
                    contextAt(fromIndex),
                    startFunction == null,
                    "Modules may only define one start function"
                )
                startFunction = it
            }
            is ElementSegment -> elementSegments.add(it)
            is DataSegment -> dataSegments.add(it)
        }
    }

    return ParseResult(
        WasmModule(
            label.astNode.takeIf { it.stringRepr != null },
            types,
            imports,
            functions,
            tables,
            memories,
            globals,
            exports,
            startFunction,
            elementSegments,
            dataSegments
        ),
        currentIndex - fromIndex
    )
}
