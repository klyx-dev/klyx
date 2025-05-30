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

import kwasm.ast.AstNode
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.token.Token
import kwasm.spectests.command.assertion.parseAssertion
import kwasm.spectests.execution.ScriptContext

/**
 * From [the docs](https://github.com/WebAssembly/spec/tree/master/interpreter#scripts):
 *
 * ```
 * cmd:
 *      <module>                        ;; define, validate, and initialize module
 *      ( register <string> <name>? )   ;; register module for imports
 *      <action>                        ;; perform action and print results
 *      <assertion>                     ;; assert result of an action
 *      <meta>                          ;; meta command
 * ```
 */
interface Command<ReturnType> : AstNode {
    fun execute(context: ScriptContext): ReturnType
}

fun List<Token>.parseAndRunCommand(
    fromIndex: Int,
    context: ScriptContext
): ParseResult<out AstNode> {
    val parsedCommand = parseScriptModule(fromIndex)
        ?: parseRegistration(fromIndex)
        ?: parseAction(fromIndex)
        ?: parseAssertion(fromIndex)
        ?: parseMeta(fromIndex)
        ?: throw ParseException("No supported command found", this[fromIndex].context)

    try {
        parsedCommand.astNode.execute(context)
    } catch (e: Throwable) {
        throw RuntimeException("Error occurred while executing command at ${this[fromIndex].context}", e)
    }

    return parsedCommand
}
