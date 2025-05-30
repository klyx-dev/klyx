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

import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Token
import kwasm.runtime.Value
import kwasm.spectests.command.Action
import kwasm.spectests.command.Command
import kwasm.spectests.command.parseAction
import kwasm.spectests.execution.ScriptContext
import kwasm.spectests.type.parseScriptResult

/**
 * (assert_return <action> <result>*)
 */
class AssertReturn(
    val action: Action,
    val expectedResults: List<Value<*>>
) : Command<Unit> {
    override fun execute(context: ScriptContext) {
        val results = action.execute(context)
        if (results.size != expectedResults.size) {
            throw AssertionError(
                "Received ${results.size} results, expected ${expectedResults.size}."
            )
        }
        val allMatch = results.zip(expectedResults).all { (actual, expected) ->
            actual.value == expected.value
        }
        if (!allMatch) {
            throw AssertionError(
                "Actual results: $results don't match expected results: $expectedResults"
            )
        }
    }
}

fun List<Token>.parseAssertReturn(fromIndex: Int): ParseResult<AssertReturn>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "assert_return")) return null
    currentIndex++

    val action = parseAction(currentIndex)
        ?: throw ParseException("Expected action", contextAt(currentIndex))
    currentIndex += action.parseLength

    val results = mutableListOf<Value<*>>()
    do {
        val result = parseScriptResult(currentIndex)
        result?.let {
            currentIndex += it.parseLength
            results += it.astNode
        }
    } while (result != null)

    if (!isClosedParen(currentIndex)) {
        throw ParseException("Expected close paren", contextAt(currentIndex))
    }
    currentIndex++

    return ParseResult(AssertReturn(action.astNode, results), currentIndex - fromIndex)
}
