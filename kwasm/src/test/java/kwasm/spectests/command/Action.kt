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
import kwasm.ast.instruction.Expression
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.instruction.parseExpression
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseIdentifier
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import kwasm.runtime.Value
import kwasm.spectests.execution.ScriptContext

/**
 * Defines an action allowed to be run during a script.
 *
 * See [the docs](https://github.com/WebAssembly/spec/tree/master/interpreter#scripts)
 */
sealed class Action : Command<List<Value<*>>> {
    /**
     * Invoke a function with the given exported name using the results of the [arguments]
     * expressions as arguments.
     *
     * (invoke <name>? <string> <expr>*)
     */
    data class Invoke(
        val moduleIdentifier: Identifier.Label?,
        val functionExportName: String,
        val arguments: List<Expression>
    ) : Action() {
        override fun execute(context: ScriptContext): List<Value<*>> {
            return context.invoke(moduleIdentifier, functionExportName, arguments)
        }
    }

    /**
     * Get the value of an exported global.
     *
     * (get <name>? <string>)
     */
    data class Get(
        val moduleIdentifier: Identifier.Label?,
        val globalExportName: String
    ) : Action() {
        override fun execute(context: ScriptContext): List<Value<*>> {
            return listOf(context.getGlobal(moduleIdentifier, globalExportName))
        }
    }
}

internal fun List<Token>.parseAction(fromIndex: Int): ParseResult<out Action>? {
    return parseInvokeAction(fromIndex) ?: parseGetAction(fromIndex)
}

internal fun List<Token>.parseInvokeAction(fromIndex: Int): ParseResult<Action.Invoke>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "invoke")) return null
    currentIndex++

    val identifier = parseIdentifier<Identifier.Label>(currentIndex, false)
    currentIndex += identifier?.parseLength ?: 0

    val exportName = parseLiteral(currentIndex, String::class)
    currentIndex += exportName.parseLength

    val arguments = mutableListOf<Expression>()
    while (!isClosedParen(currentIndex)) {
        val expression = parseExpression(currentIndex)
        currentIndex += expression.parseLength
        arguments += expression.astNode
    }

    if (!isClosedParen(currentIndex)) {
        throw ParseException("Expected closing paren", contextAt(currentIndex))
    }
    currentIndex++

    return ParseResult(
        Action.Invoke(identifier?.astNode, exportName.astNode.value, arguments),
        currentIndex - fromIndex
    )
}

internal fun List<Token>.parseGetAction(fromIndex: Int): ParseResult<Action.Get>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "get")) return null
    currentIndex++

    val identifier = parseIdentifier<Identifier.Label>(currentIndex, false)
    currentIndex += identifier?.parseLength ?: 0

    val exportName = parseLiteral(currentIndex, String::class)
    currentIndex += exportName.parseLength

    if (!isClosedParen(currentIndex)) {
        throw ParseException("Expected closing paren", contextAt(currentIndex))
    }
    currentIndex++
    return ParseResult(
        Action.Get(identifier?.astNode, exportName.astNode.value),
        currentIndex - fromIndex
    )
}
