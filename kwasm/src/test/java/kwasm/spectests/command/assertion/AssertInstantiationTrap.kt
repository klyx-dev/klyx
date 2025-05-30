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

package kwasm.spectests.command.assertion

import com.google.common.truth.Truth.assertThat
import kwasm.KWasmRuntimeException
import kwasm.ast.AstNode
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import kwasm.spectests.command.Command
import kwasm.spectests.command.ScriptModule
import kwasm.spectests.command.parseScriptModule
import kwasm.spectests.execution.ScriptContext
import org.junit.Assert.assertThrows

class AssertInstantiationTrap(
    val action: ScriptModule,
    val messageContains: String
) : AstNode, Command<Unit> {
    override fun execute(context: ScriptContext) {
        val e = assertThrows(KWasmRuntimeException::class.java) {
            action.execute(context)
        }
        assertThat(e).hasMessageThat().contains(messageContains)
    }
}

// (assert_invalid <module> <string>)
fun List<Token>.parseAssertInstantiationTrap(
    fromIndex: Int
): ParseResult<AssertInstantiationTrap>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "assert_trap")) return null
    currentIndex++

    val scriptModule = parseScriptModule(currentIndex) ?: return null
    currentIndex += scriptModule.parseLength

    val message = parseLiteral(currentIndex, String::class)
    currentIndex += message.parseLength

    if (!isClosedParen(currentIndex)) {
        throw ParseException("Expected close paren", contextAt(currentIndex))
    }
    currentIndex++

    return ParseResult(
        AssertInstantiationTrap(scriptModule.astNode, message.astNode.value),
        currentIndex - fromIndex
    )
}
