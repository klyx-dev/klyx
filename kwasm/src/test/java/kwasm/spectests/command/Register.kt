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
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseIdentifier
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import kwasm.spectests.execution.ScriptContext

/**
 * Registers an identified module (one with a defined [Identifier.Label]) for use as a provider of
 * imports for other modules. If no identifier is provided, the most recently-read module will
 * be used.
 *
 * (register <string> <name>?)
 */
data class Register(
    val importableName: String,
    val moduleId: Identifier.Label?
) : Command<Unit> {
    override fun execute(context: ScriptContext) {
        val moduleToRegister = if (moduleId != null) {
            context.programBuilder.modulesInOrder.removeLast()
            requireNotNull(context.modules[moduleId])
        } else {
            context.programBuilder.modulesInOrder.removeLast().second
        }
        context.programBuilder.parsedModules.remove(moduleToRegister.identifier?.stringRepr ?: "")
        context.programBuilder.withModule(importableName, moduleToRegister)
        context.build()
    }
}

fun List<Token>.parseRegistration(fromIndex: Int): ParseResult<Register>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "register")) return null
    currentIndex++

    val importName = parseLiteral(currentIndex, String::class)
    currentIndex += importName.parseLength

    val moduleName = parseIdentifier<Identifier.Label>(currentIndex, false)
    currentIndex += moduleName?.parseLength ?: 0

    if (!isClosedParen(currentIndex)) {
        throw ParseException("Expected closing paren", this[currentIndex].context)
    }
    currentIndex++

    return ParseResult(
        Register(importName.astNode.value, moduleName?.astNode),
        currentIndex - fromIndex
    )
}
