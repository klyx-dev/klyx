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

package kwasm.spectests.command

import kwasm.ast.Identifier
import kwasm.ast.module.WasmModule
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.module.readModule
import kwasm.format.text.ParseResult
import kwasm.format.text.Tokenizer
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.module.parseDataString
import kwasm.format.text.module.parseModule
import kwasm.format.text.parseIdentifier
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import kwasm.spectests.execution.ScriptContext
import java.io.ByteArrayInputStream

/**
 * Defines a module
 */
sealed class ScriptModule : Command<Unit> {
    abstract val identifier: Identifier.Label?

    data class Normal(val module: WasmModule) : ScriptModule() {
        override val identifier: Identifier.Label? = module.identifier

        override fun execute(context: ScriptContext) {
            context.modules[identifier] = module
            context.programBuilder.withModule(identifier?.stringRepr ?: "", module)
            context.build()
        }
    }

    /**
     * (module <name>? quote <string>*)
     */
    data class Quoted(
        override val identifier: Identifier.Label?,
        val parseContext: ParseContext,
        val source: String
    ) : ScriptModule() {
        override fun execute(context: ScriptContext) {
            val moduleSource = if (source.startsWith("(module")) source else "(module $source)"
            val module = Tokenizer().tokenize(
                moduleSource,
                parseContext
            ).parseModule(0)!!.astNode

            context.modules[identifier] = module
            context.programBuilder.withModule("", moduleSource)
            context.build()
        }
    }

    /**
     * (module <name>? binary <string>*)
     */
    class Binary(
        override val identifier: Identifier.Label?,
        val parseContext: ParseContext,
        val source: ByteArray
    ) : ScriptModule() {
        override fun execute(context: ScriptContext) {
            context.modules[identifier] =
                BinaryParser(ByteArrayInputStream(source), parseContext).readModule()
            context.programBuilder.withBinaryModule("", ByteArrayInputStream(source))
            context.build()
        }
    }
}

fun List<Token>.parseScriptModule(fromIndex: Int): ParseResult<ScriptModule>? {
    var parseException: ParseException? = null
    val normalModule = try { parseModule(fromIndex) } catch (e: ParseException) {
        parseException = e
        null
    }
    if (normalModule != null) {
        return ParseResult(ScriptModule.Normal(normalModule.astNode), normalModule.parseLength)
    }

    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "module")) return null
    currentIndex++

    val identifier = parseIdentifier<Identifier.Label>(currentIndex, false)
    currentIndex += identifier?.parseLength ?: 0

    val indexOfStart = currentIndex
    val module: ScriptModule = if (isKeyword(currentIndex, "quote")) {
        currentIndex++
        val moduleStrings = mutableListOf<String>()
        while (!isClosedParen(currentIndex)) {
            val literal = parseLiteral(currentIndex, String::class)
            moduleStrings.add(literal.astNode.value)
            currentIndex += literal.parseLength
        }
        ScriptModule.Quoted(
            identifier?.astNode,
            this[indexOfStart].context!!,
            moduleStrings.joinToString(" ")
        )
    } else if (isKeyword(currentIndex, "binary")) {
        currentIndex++
        val (bytes, length) = parseDataString(currentIndex)
        currentIndex += length
        ScriptModule.Binary(identifier?.astNode, this[indexOfStart].context!!, bytes)
    } else {
        throw parseException
            ?: ParseException("Unsupported module declaration", this[currentIndex].context)
    }
    if (!isClosedParen(currentIndex)) {
        throw ParseException("Expected closing paren", this[currentIndex].context)
    }
    currentIndex++

    return ParseResult(module, currentIndex - fromIndex)
}
